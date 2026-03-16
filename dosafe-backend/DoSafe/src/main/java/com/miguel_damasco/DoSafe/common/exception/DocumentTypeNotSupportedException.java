package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public class DocumentTypeNotSupportedException extends DoSafeException {

    public DocumentTypeNotSupportedException(DocumentTypeEnum type) {
        super("No extractor found for document type '" + type + "'", HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_TYPE_NOT_SUPPORTED");
    }
}
