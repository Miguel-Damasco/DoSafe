package com.miguel_damasco.DoSafe.document.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.extraction.ExpirationDateExtractor;

import com.miguel_damasco.DoSafe.common.exception.DocumentTypeNotSupportedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ExpirationDateExtractorSelector {

    private final List<ExpirationDateExtractor> extractorsList;

    public ExpirationDateExtractor selectExtractor(DocumentModel pDocument) {

        return extractorsList.stream()
                            .filter(document -> document.supports(pDocument.getType()))
                            .findFirst()
                            .orElseThrow(() -> new DocumentTypeNotSupportedException(pDocument.getType()));
    }
}
