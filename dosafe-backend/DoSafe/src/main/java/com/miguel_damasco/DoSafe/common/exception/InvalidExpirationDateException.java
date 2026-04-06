package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidExpirationDateException extends DoSafeException {

    public InvalidExpirationDateException() {
        super("Expiration date cannot be in the past", HttpStatus.BAD_REQUEST, "INVALID_EXPIRATION_DATE");
    }
}
