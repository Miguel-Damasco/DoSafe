package com.miguel_damasco.DoSafe.alert.dto;

import java.util.List;

public record AlertPageResponseDTO(
        List<AlertResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {}
