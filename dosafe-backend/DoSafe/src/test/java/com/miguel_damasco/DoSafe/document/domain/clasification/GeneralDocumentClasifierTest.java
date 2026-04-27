package com.miguel_damasco.DoSafe.document.domain.clasification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

// Unit test — no Spring context needed. We instantiate the class directly.
// This keeps the test fast and isolated from the rest of the application.
class GeneralDocumentClasifierTest {

    private GeneralDocumentClasifier clasifier;

    @BeforeEach
    void setUp() {
        // A fresh instance before each test so tests don't share state.
        clasifier = new GeneralDocumentClasifier();
    }

    @Test
    void classify_shouldReturnIdentityCard_whenLinesContainIdentityCardKeyword() {
        // Given — OCR lines from an identity card document
        List<String> lines = List.of("República Oriental del Uruguay", "Documento de Identidad", "12345678");

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then
        assertThat(result).isEqualTo(DocumentTypeEnum.IDENTITY_CARD);
    }

    @Test
    void classify_shouldReturnPassport_whenLinesContainPassportKeyword() {
        // Given — OCR lines from a passport
        List<String> lines = List.of("URUGUAY", "PASAPORTE/", "PASSPORT");

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then
        assertThat(result).isEqualTo(DocumentTypeEnum.PASSPORT);
    }

    @Test
    void classify_shouldReturnOther_whenNoKeywordIsFound() {
        // Given — OCR lines that don't match any known document type
        List<String> lines = List.of("some random text", "another line", "nothing useful");

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then
        assertThat(result).isEqualTo(DocumentTypeEnum.OTHER);
    }

    @Test
    void classify_shouldReturnOther_whenLinesAreEmpty() {
        // Given — empty OCR result (Textract found nothing to classify)
        List<String> lines = List.of();

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then — unrecognized documents fall back to OTHER, never null
        assertThat(result).isEqualTo(DocumentTypeEnum.OTHER);
    }

    @Test
    void classify_shouldFindKeyword_evenWhenItIsNotOnTheFirstLine() {
        // Given — keyword appears after several unrelated lines
        List<String> lines = List.of("some noise", "more noise", "Documento de Identidad", "12345678");

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then
        assertThat(result).isEqualTo(DocumentTypeEnum.IDENTITY_CARD);
    }

    @Test
    void classify_shouldStopAtFirstMatch_andNotOverrideWithOther() {
        // Given — keyword appears first, then unrelated lines follow
        // The classifier should break on the first non-OTHER match
        // and not continue iterating to override the result with OTHER.
        List<String> lines = List.of("PASAPORTE/", "some line after", "another line after");

        // When
        DocumentTypeEnum result = clasifier.classify(lines);

        // Then
        assertThat(result).isEqualTo(DocumentTypeEnum.PASSPORT);
    }
}
