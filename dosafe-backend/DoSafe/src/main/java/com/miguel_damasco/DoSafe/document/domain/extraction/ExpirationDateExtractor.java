package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public interface ExpirationDateExtractor {

    boolean supports(DocumentTypeEnum type);

    LocalDate extract(List<String> pLines, UUID pDocumentId, long pIdUser);
}
