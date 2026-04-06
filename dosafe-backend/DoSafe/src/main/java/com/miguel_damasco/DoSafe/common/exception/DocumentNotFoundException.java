package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends DoSafeException {

    public DocumentNotFoundException(String documentId) {
        super("Document '" + documentId + "' not found", HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND");
    }
}
