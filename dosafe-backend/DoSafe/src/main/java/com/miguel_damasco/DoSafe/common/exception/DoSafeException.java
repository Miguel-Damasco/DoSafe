package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class DoSafeException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public DoSafeException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
