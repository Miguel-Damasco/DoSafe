package com.miguel_damasco.DoSafe.document.infraestructure.ocr.sqs;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miguel_damasco.DoSafe.document.domain.DocumentId;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.sns.SnsNotification;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract.TextractNotificationMessage;
import com.miguel_damasco.DoSafe.document.service.ProcessTextractResultService;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

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

        System.out.println();
        System.out.println("Poll iniciada!");
        System.out.println();

        ReceiveMessageRequest request =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build();

        List<Message> messages =
                sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            process(message);
            delete(message);
        }
    }

    private void process(Message sqsMessage) throws JsonMappingException, JsonProcessingException {

        System.out.println();
        System.out.println("Procesando mensaje!");
        System.out.println();

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

        DocumentId documentId =
                extractDocumentId(
                        textract.getDocumentLocation().getS3ObjectName()
                );

        processService.execute(
                documentId,
                textract.getJobId()
        );
    }

    private DocumentId extractDocumentId(String s3ObjectName) {

        String[] parts = s3ObjectName.split("/");

        if(parts.length < 3) {
            throw new IllegalStateException("Invalid S3 object name: " + s3ObjectName);
        }

        String rawId = parts[2].substring(0, parts[2].length() - 4);

        System.out.println();
        System.out.println("RawId: " + rawId);
        System.out.println();

        return new DocumentId(UUID.fromString(rawId));
    }


    private void delete(Message message) {

        System.out.println();
        System.out.println("Eliminando mensaje de la queue!");
        System.out.println();

        sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
        );
    }
}
