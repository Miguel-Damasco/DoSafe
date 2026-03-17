package com.miguel_damasco.DoSafe.document.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Unit test — no Spring context needed. Pure logic, instantiated directly.
class PassportDateExtractorTest {

    private PassportDateExtractor extractor;

    // Fixed values for pDocumentId and pUserId — they are only used for logging,
    // so their values don't affect the extraction result.
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        extractor = new PassportDateExtractor();
    }

    @Test
    void extract_shouldReturnNull_whenLinesAreEmpty() {
        // Given
        List<String> lines = List.of();

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void extract_shouldReturnNull_whenVencimentoIsNotFound() {
        // Given — OCR lines with no "VENCIMENTO" keyword
        List<String> lines = List.of("URUGUAY", "PASAPORTE", "15 Ene 2030");

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void extract_shouldReturnNull_whenNotEnoughLinesAfterVencimento() {
        // Given — "VENCIMENTO" is found but there are fewer than 2 lines after it,
        // so the date line (i+2) doesn't exist.
        List<String> lines = List.of("VENCIMENTO", "only one line after");

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void extract_shouldReturnNull_whenMonthAbbreviationIsUnrecognized() {
        // Given — date line contains an unknown month abbreviation
        List<String> lines = List.of("VENCIMENTO", "ignored", "15 Xyz 2030");

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void extract_shouldReturnCorrectDate_whenVencimentoIsNotOnFirstLine() {
        // Given — "VENCIMENTO" appears after several unrelated OCR lines
        List<String> lines = List.of(
            "URUGUAY",
            "PASAPORTE",
            "NOMBRES / GIVEN NAMES",
            "VENCIMENTO",
            "ignored label",
            "15 Ene 2030"
        );

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isEqualTo(LocalDate.of(2030, 1, 15));
    }

    // Parameterized test — verifies that all 12 month abbreviations are parsed correctly.
    // Each argument is: (dateString, expectedMonth)
    @ParameterizedTest(name = "{0} → month {1}")
    @MethodSource("monthProvider")
    void extract_shouldParseAllMonths_correctly(String dateString, int expectedMonth) {
        // Given — OCR lines with the Uruguayan passport format
        List<String> lines = List.of("VENCIMENTO", "ignored", dateString);

        // When
        LocalDate result = extractor.extract(lines, DOCUMENT_ID, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMonthValue()).isEqualTo(expectedMonth);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getYear()).isEqualTo(2030);
    }

    static Stream<Arguments> monthProvider() {
        return Stream.of(
            Arguments.of("15 Ene 2030", 1),
            Arguments.of("15 Feb 2030", 2),
            Arguments.of("15 Mar 2030", 3),
            Arguments.of("15 Abr 2030", 4),
            Arguments.of("15 May 2030", 5),
            Arguments.of("15 Jun 2030", 6),
            Arguments.of("15 Jul 2030", 7),
            Arguments.of("15 Ago 2030", 8),
            Arguments.of("15 Sep 2030", 9),
            Arguments.of("15 Oct 2030", 10),
            Arguments.of("15 Nov 2030", 11),
            Arguments.of("15 Dic 2030", 12)
        );
    }
}
