package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

@Component
public class OtherExpirationDateExtractor implements ExpirationDateExtractor {

    @Override
    public boolean supports(DocumentTypeEnum type) {
        return type == DocumentTypeEnum.OTHER;
    }

    @Override
    public LocalDate extract(List<String> pLines, UUID pDocumentId, long pUserId) {
       return null;
    }
    
}
