package com.miguel_damasco.DoSafe.common.apiResponse;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata present in every API response")
public record Meta(
        @Schema(description = "Whether the operation succeeded", example = "true")
        boolean success,
        @Schema(description = "HTTP status code", example = "200")
        int statusCode,
        @Schema(description = "Human-readable message", example = "Login successfully!")
        String message,
        @Schema(description = "UTC timestamp of the response")
        Instant timestamp
) {
    
    public static Meta of(boolean pSuccess, int pStatusCode, String pMessage) {
        return new Meta(pSuccess, pStatusCode, pMessage, Instant.now());
    }
}
