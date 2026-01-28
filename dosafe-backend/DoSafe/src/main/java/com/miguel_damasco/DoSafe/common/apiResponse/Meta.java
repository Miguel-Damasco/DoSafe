package com.miguel_damasco.DoSafe.common.apiResponse;

import java.time.Instant;

public record Meta(
        boolean success,
        int statusCode,
        String message,
        Instant timestamp
) {
    
    public static Meta of(boolean pSuccess, int pStatusCode, String pMessage) {
        return new Meta(pSuccess, pStatusCode, pMessage, Instant.now());
    }
}
