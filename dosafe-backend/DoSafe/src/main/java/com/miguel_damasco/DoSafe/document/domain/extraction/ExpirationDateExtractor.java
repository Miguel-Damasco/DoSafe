package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;

import org.w3c.dom.DocumentType;

public interface ExpirationDateExtractor {

    boolean supports(DocumentType type);

    LocalDate extract(List<String> pLines);
}
