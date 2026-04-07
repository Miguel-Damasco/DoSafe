package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DoSafeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDoSafeException(DoSafeException ex) {
        log.warn("Application error errorCode={} message={}", ex.getErrorCode(), ex.getMessage());
        return buildError(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed message={}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload too large message={}", ex.getMessage());
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File exceeds the maximum allowed size of 5MB");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body message={}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Request body is missing or malformed");
    }

    // Thrown by Spring MVC when a @RequestParam cannot be converted to the expected type.
    // Example: GET /my-documents/by-type?type=INVALIDO — 'INVALIDO' is not a valid DocumentTypeEnum.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        log.warn("Invalid request parameter param={} value={}", paramName, invalidValue);

        // If the expected type is an enum, list the valid values in the error message.
        Class<?> requiredType = ex.getRequiredType();
        String details;
        if (requiredType != null && requiredType.isEnum()) {
            String validValues = java.util.Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
            details = String.format("Invalid value '%s' for parameter '%s'. Valid values: %s", invalidValue, paramName, validValues);
        } else {
            details = String.format("Invalid value '%s' for parameter '%s'", invalidValue, paramName);
        }

        return buildError(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error message={}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String errorCode, String details) {
        ApiResponse<Void> body = ApiResponses.error(errorCode, details, status.value(), status.getReasonPhrase());
        return ResponseEntity.status(status).body(body);
    }
}
