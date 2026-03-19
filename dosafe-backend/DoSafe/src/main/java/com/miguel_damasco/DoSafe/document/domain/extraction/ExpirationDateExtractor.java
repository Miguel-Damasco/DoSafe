package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public interface ExpirationDateExtractor {

    boolean supports(DocumentTypeEnum type);

    // Returns null in two valid scenarios:
    //   1. The document type is recognized but the OCR output doesn't contain a parseable date.
    //   2. The document type is unrecognized (OTHER) — no extraction strategy exists.
    // In both cases the user will be prompted to enter the expiration date manually.
    LocalDate extract(List<String> pLines, UUID pDocumentId, long pIdUser);
}
