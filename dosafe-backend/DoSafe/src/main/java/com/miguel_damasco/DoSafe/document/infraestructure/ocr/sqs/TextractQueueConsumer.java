package com.miguel_damasco.DoSafe.document.infraestructure.ocr.sqs;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miguel_damasco.DoSafe.common.correlationId.CorrelationIdHolder;
import com.miguel_damasco.DoSafe.document.domain.DocumentId;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.sns.SnsNotification;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract.TextractNotificationMessage;
import com.miguel_damasco.DoSafe.document.service.ProcessTextractResultService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@RequiredArgsConstructor
@Service
public class TextractQueueConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessTextractResultService processService;

    // Injected from AsyncConfig — bounded pool of 5 threads, one per SQS message.
    private final ExecutorService textractExecutor;

    @Value("${aws.sqs.document-text-extracted-queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000)
    public void pollQueue() {

        log.info("Starting poll");

        ReceiveMessageRequest request =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        if (messages.isEmpty()) {
            log.debug("No messages in queue");
            return;
        }

        log.info("Processing batch messageCount={}", messages.size());

        // Submit each message to a separate thread so the whole batch runs concurrently.
        // CorrelationIdHolder.wrap() captures the correlationId of this (scheduling) thread
        // and restores it inside each worker thread before the task runs.
        List<CompletableFuture<Void>> futures = messages.stream()
                .map(message -> CompletableFuture.runAsync(
                        CorrelationIdHolder.wrap(() -> processAndDelete(message)),
                        textractExecutor
                ))
                .toList();

        // Block until every message in this batch has been processed.
        // This ensures the next scheduled poll doesn't start until the current one is done,
        // preventing the same messages from being picked up twice.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Batch completed messageCount={}", messages.size());
    }

    // Processes a message and deletes it from SQS if successful.
    // Both steps run in a worker thread from the textractExecutor pool.
    private void processAndDelete(Message message) {
        process(message);
        delete(message);
    }

    private void process(Message sqsMessage) {

        try {
            log.info("Proccessing OCR message");

            SnsNotification sns =
                    objectMapper.readValue(
                            sqsMessage.body(),
                            SnsNotification.class
                    );

            TextractNotificationMessage textract =
                    objectMapper.readValue(
                            sns.getMessage(),
                            TextractNotificationMessage.class
                    );

            if (!"SUCCEEDED".equals(textract.getStatus())) {
                return;
            }

            String correlationId = textract.getJobTag();

            if (correlationId != null) {
                // Override the correlationId with the one stored in the Textract jobTag.
                // This restores the original request's correlationId so all async log lines
                // are traceable back to the HTTP request that triggered the upload.
                CorrelationIdHolder.set(correlationId);
                log.info("Correlation ID set from Textract jobTag correlationId={}", correlationId);
            } else {
                log.warn("Textract jobTag is null, processing without correlationId");
            }

            DocumentId documentId = extractDocumentId(textract.getDocumentLocation().getS3ObjectName());

            processService.execute(documentId, textract.getJobId());

        } catch (Exception e) {
            log.warn("Can't procces SQS message", e);
        }
    }

    private DocumentId extractDocumentId(String s3ObjectName) {

        log.info("Starting document id extraction");

        String[] parts = s3ObjectName.split("/");

        if (parts.length < 3) {
            throw new IllegalStateException("Invalid S3 object name: " + s3ObjectName);
        }

        String rawId = parts[2].substring(0, parts[2].length() - 4);

        log.info("Document extraction end documentId={}", rawId);

        return new DocumentId(UUID.fromString(rawId));
    }

    private void delete(Message message) {

        log.info("Sarting message deletion");

        sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
        );

        log.info("finish message deletion");
    }
}
