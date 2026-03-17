package com.miguel_damasco.DoSafe.common.apiResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error detail present only in error responses")
public record ErrorInfo(
    @Schema(description = "Machine-readable error code", example = "USER_ALREADY_EXISTS")
    String code,
    @Schema(description = "Human-readable explanation", example = "Username 'miguel' is already taken")
    String details
) {}
