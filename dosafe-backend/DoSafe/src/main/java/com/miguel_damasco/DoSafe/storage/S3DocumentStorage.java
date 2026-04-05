package com.miguel_damasco.DoSafe.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.common.correlationId.CorrelationIdHolder;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Component
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3DocumentStorage(S3Client pS3Client, S3Presigner s3Presigner) {
        this.s3Client = pS3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String upload(long userId, String pDocumentId, InputStream pContent, long pSize, String pContentType) {

        log.info("Starting upload documentId={} userId={}", pDocumentId, userId);

        String key = generateKey(userId, pDocumentId);

         String correlationId = CorrelationIdHolder.get();

        PutObjectRequest request = PutObjectRequest.builder()
                                                    .bucket(bucketName)
                                                    .key(key)
                                                    .contentType(pContentType)
                                                    .metadata(Map.of("correlation-id", correlationId))
                                                    .build();

        PutObjectResponse response = this.s3Client.putObject(request, RequestBody.fromInputStream(pContent, pSize));

        log.info("Upload finish documentId={} userId={}", pDocumentId, userId);

        return key;
    }

    @Override
    public URL generateDownloadUrl(String pKey, Duration pTtl) {

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(pKey)
                                                            .build();

        GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
                                                                                .signatureDuration(pTtl)
                                                                                .getObjectRequest(objectRequest)
                                                                                .build();

        PresignedGetObjectRequest presignedRequest = this.s3Presigner.presignGetObject(objectPresignRequest);

        // The full presigned URL is not logged because it contains AWS credentials
        // in the query string. Logging it would expose those credentials in any
        // log aggregation system.
        log.info("Presigned URL generated key={} ttl={}", pKey, pTtl);

        return presignedRequest.url();
    }

    @Override
    public void delete(String pKey) {
       
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(pKey)
                                                            .build();

        this.s3Client.deleteObject(request);
    }
    
    private String generateKey(long pUserId, String pDocumentId) {
        return String.format("users/%s/%s.pdf", pUserId, pDocumentId);
    }
}
