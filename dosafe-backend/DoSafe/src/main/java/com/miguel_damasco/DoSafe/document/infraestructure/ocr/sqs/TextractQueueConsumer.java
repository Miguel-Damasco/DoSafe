package com.miguel_damasco.DoSafe.document.infraestructure.ocr.sqs;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miguel_damasco.DoSafe.common.correlationId.CorrelationIdHolder;
import com.miguel_damasco.DoSafe.document.domain.DocumentId;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract.TextractNotificationMessage;
import com.miguel_damasco.DoSafe.document.service.ProcessTextractResultService;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.sns.SnsNotification;

@Slf4j
@RequiredArgsConstructor
@Service
public class TextractQueueConsumer {
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessTextractResultService processService;

    @Value("${aws.sqs.document-text-extracted-queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000)
    public void pollQueue() throws JsonMappingException, JsonProcessingException {

        log.info("Starting poll");

        ReceiveMessageRequest request =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            process(message);
            delete(message);
        }
    }

    private void process(Message sqsMessage) throws JsonMappingException, JsonProcessingException {


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

                System.out.println();
                System.out.println("Job tag: " + correlationId);
                System.out.println();

                if(correlationId != null) CorrelationIdHolder.set(correlationId);

                DocumentId documentId = extractDocumentId(textract.getDocumentLocation().getS3ObjectName());

                processService.execute(documentId, textract.getJobId());

        } catch (Exception e) {

               log.warn("Can't procces SQS message");
        }
    }

    private DocumentId extractDocumentId(String s3ObjectName) {

        log.info("Starting document id extraction");

        String[] parts = s3ObjectName.split("/");

        if(parts.length < 3) {
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

        CorrelationIdHolder.clear();
    }
}
