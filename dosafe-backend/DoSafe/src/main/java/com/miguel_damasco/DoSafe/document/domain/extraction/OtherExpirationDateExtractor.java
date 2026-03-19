package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

// Null Object implementation of ExpirationDateExtractor for unrecognized document types.
//
// When the classifier cannot identify the document type, ExpirationDateExtractorSelector
// falls back to this extractor. Since the document structure is unknown, there is no
// reliable way to locate an expiration date in the OCR output — attempting to parse
// arbitrary lines would produce false positives.
//
// Returning null is intentional and consistent with the interface contract:
// the user will be prompted to enter the expiration date manually.
@Component
public class OtherExpirationDateExtractor implements ExpirationDateExtractor {

    @Override
    public boolean supports(DocumentTypeEnum type) {
        return type == DocumentTypeEnum.OTHER;
    }

    @Override
    public LocalDate extract(List<String> pLines, UUID pDocumentId, long pUserId) {
        // Intentionally returns null — document type is unrecognized, extraction is not possible.
        return null;
    }
}
