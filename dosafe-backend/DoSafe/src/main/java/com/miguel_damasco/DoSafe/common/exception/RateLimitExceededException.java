package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends DoSafeException {

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
}
