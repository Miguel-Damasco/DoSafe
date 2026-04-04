package com.miguel_damasco.DoSafe.document.dto.response;

import java.util.List;

// Wraps a paginated list of documents with the metadata the client needs
// to render a paginator: current page, total pages, total count, etc.
public record DocumentPageResponseDTO(
        List<DocumentSummaryResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {}
