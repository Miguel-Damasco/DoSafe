package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

// Thrown when a user tries to modify a document that belongs to another user.
public class DocumentOwnershipException extends DoSafeException {

    public DocumentOwnershipException() {
        super("You do not have permission to modify this document", HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED");
    }
}
