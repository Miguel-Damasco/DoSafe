package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public interface ExpirationDateExtractor {

    boolean supports(DocumentTypeEnum type);

    LocalDate extract(List<String> pLines);
}
