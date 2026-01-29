package com.miguel_damasco.DoSafe.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

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
    public void upload(String pKey, InputStream pContent, long pSize, String pContentType) {

        PutObjectRequest request = PutObjectRequest.builder()
                                                    .bucket(bucketName)
                                                    .key(pKey)
                                                    .contentType(pContentType)
                                                    .build();

        PutObjectResponse response = this.s3Client.putObject(request, RequestBody.fromInputStream(pContent, pSize));

        System.out.println(String.format("Metadata [%s]", response.responseMetadata()));
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

        System.out.println(String.format("Presigned URL [%s]", presignedRequest.url().toString()));
        System.out.println(String.format("HTTP method [%s]", presignedRequest.httpRequest().method()));

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
    
}
