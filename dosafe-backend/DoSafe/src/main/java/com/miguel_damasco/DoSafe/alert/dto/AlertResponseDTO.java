package com.miguel_damasco.DoSafe.alert.dto;

import java.time.Instant;
import java.time.LocalDate;

// Represents a single alert in the user's notification panel.
// 'read' is derived from readAt — true means the user already saw it.
public record AlertResponseDTO(
        String id,
        String documentId,
        String documentType,
        String documentFilename,
        LocalDate expireAt,
        Instant sentAt,
        boolean read) {}
