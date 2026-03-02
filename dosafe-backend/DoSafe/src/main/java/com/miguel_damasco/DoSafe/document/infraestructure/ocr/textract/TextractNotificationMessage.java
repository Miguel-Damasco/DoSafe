package com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextractNotificationMessage {

    @JsonProperty("JobId")
    private String jobId;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("API")
    private String api;

    @JsonProperty("DocumentLocation")
    private DocumentLocation documentLocation;

    @Data
    public static class DocumentLocation {

        @JsonProperty("S3ObjectName")
        private String s3ObjectName;

        @JsonProperty("S3Bucket")
        private String s3Bucket;
    }
}
