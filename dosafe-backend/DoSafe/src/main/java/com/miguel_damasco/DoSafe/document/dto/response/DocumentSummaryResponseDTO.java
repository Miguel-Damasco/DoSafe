package com.miguel_damasco.DoSafe.document.dto.response;

import java.time.Instant;
import java.time.LocalDate;

// Represents a single document in a paginated list.
// Includes 'status' so the user knows if their document is still being processed.
// 'type' is nullable — it is null while OCR is still running (status = PROCESSING).
// 'downloadUrl' is nullable — it is null when s3Key is not yet set (status = PROCESSING).
public record DocumentSummaryResponseDTO(
        String id,
        String type,
        String status,
        String originalFilename,
        Instant createdAt,
        LocalDate expireAt,
        String downloadUrl) {}
