package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends DoSafeException {

    public InvalidVerificationTokenException() {
        super(
            "The verification token is invalid, expired, or has already been used.",
            HttpStatus.BAD_REQUEST,
            "INVALID_VERIFICATION_TOKEN"
        );
    }
}
