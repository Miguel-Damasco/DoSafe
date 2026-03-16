package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class DocumentProcessingException extends DoSafeException {

    public DocumentProcessingException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_PROCESSING_ERROR");
    }
}
