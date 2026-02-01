package com.miguel_damasco.DoSafe.document.dto.mapper;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;

public class DocumentResponseMapper {
    
    public static DocumentUploadResponseDTO toDto(DocumentModel pDocument) {

        return new DocumentUploadResponseDTO(pDocument.getId().
                                                toString(), 
                                                pDocument.getType().name(), 
                                                pDocument.getOriginalFilename(), 
                                                pDocument.getCreatedAt(), 
                                                pDocument.getExpireAt());
    }
}
