package com.miguel_damasco.DoSafe.common.apiResponse;

import io.swagger.v3.oas.annotations.media.Schema;

public sealed interface ApiResponse<T>
                            permits ApiResponse.Success, ApiResponse.Error {

    Meta meta();

    // Success<T> wraps the actual response payload in the "data" field.
    // The generic T cannot be resolved statically by Springdoc — the concrete
    // type (e.g. LoginResponseDTO) is visible in the actual response body via "Try it out".
    @Schema(description = "Successful API response")
    public record Success<T>(
        @Schema(description = "Response payload — type varies per endpoint")
        T data,
        Meta meta
    ) implements ApiResponse<T> {}

    @Schema(description = "Error API response")
    public record Error(
        ErrorInfo error,
        Meta meta
    ) implements ApiResponse<Void> {}
}
