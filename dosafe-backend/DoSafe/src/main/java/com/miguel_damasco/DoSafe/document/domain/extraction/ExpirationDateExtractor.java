package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public interface ExpirationDateExtractor {

    boolean supports(DocumentTypeEnum type);

    // Returns null if the date cannot be extracted — the user will provide it manually.
    LocalDate extract(List<String> pLines, UUID pDocumentId, long pIdUser);
}
