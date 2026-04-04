package com.miguel_damasco.DoSafe.document.dto.mapper;

import java.util.List;

import org.springframework.data.domain.Page;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentPageResponseDTO;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentSummaryResponseDTO;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;

public class DocumentResponseMapper {

    public static DocumentUploadResponseDTO toDto(DocumentModel pDocument) {

        return new DocumentUploadResponseDTO(pDocument.getId().toString(),
                                             pDocument.getType().name(),
                                             pDocument.getOriginalFilename(),
                                             pDocument.getCreatedAt(),
                                             pDocument.getExpireAt());
    }

    // Maps a single DocumentModel to the summary DTO used in list responses.
    // 'type' may be null while OCR is still running, so we guard with a null check.
    public static DocumentSummaryResponseDTO toSummaryDto(DocumentModel pDocument) {

        String type = pDocument.getType() != null ? pDocument.getType().name() : null;

        return new DocumentSummaryResponseDTO(pDocument.getId().toString(),
                                              type,
                                              pDocument.getStatus().name(),
                                              pDocument.getOriginalFilename(),
                                              pDocument.getCreatedAt(),
                                              pDocument.getExpireAt());
    }

    // Converts a Spring Data Page into the paginated DTO the controller returns.
    // Extracts the pagination metadata (page number, total pages, etc.) from the
    // Page object so the client can render a paginator without a second request.
    public static DocumentPageResponseDTO toPageDto(Page<DocumentModel> pPage) {

        List<DocumentSummaryResponseDTO> content = pPage.getContent()
                .stream()
                .map(DocumentResponseMapper::toSummaryDto)
                .toList();

        return new DocumentPageResponseDTO(content,
                                           pPage.getNumber(),
                                           pPage.getSize(),
                                           pPage.getTotalElements(),
                                           pPage.getTotalPages(),
                                           pPage.isLast());
    }

    private DocumentResponseMapper() {}
}
