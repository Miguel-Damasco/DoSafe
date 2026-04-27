package com.miguel_damasco.DoSafe.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.miguel_damasco.DoSafe.document.domain.DocumentId;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.domain.clasification.GeneralDocumentClasifier;
import com.miguel_damasco.DoSafe.document.domain.extraction.ExpirationDateExtractor;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract.TextractClientAdapter;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.Answers;

@ExtendWith(MockitoExtension.class)
class ProcessTextractResultServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TextractClientAdapter textractClientAdapter;

    @Mock
    private GeneralDocumentClasifier generalDocumentClasifier;

    @Mock
    private ExpirationDateExtractorSelector expirationDateExtractorSelector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private ProcessTextractResultService processTextractResultService;

    // -------------------------------------------------------------------------
    // execute() — happy path
    // -------------------------------------------------------------------------

    @Test
    void execute_shouldMarkDocumentAsProcessed_whenTextractSucceeds() {
        // Given
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSING);
        List<String> ocrLines = List.of("Documento de Identidad", "VENCIMIENTO 31 DIC 2025");
        LocalDate expirationDate = LocalDate.of(2025, 12, 31);

        ExpirationDateExtractor extractor = mock(ExpirationDateExtractor.class);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(textractClientAdapter.getLines("job-123")).thenReturn(ocrLines);
        when(generalDocumentClasifier.classify(ocrLines)).thenReturn(DocumentTypeEnum.IDENTITY_CARD);
        when(expirationDateExtractorSelector.selectExtractor(doc)).thenReturn(extractor);
        when(extractor.extract(eq(ocrLines), eq(docId), anyLong())).thenReturn(expirationDate);

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-123");

        // Then — document must end up PROCESSED with type and expireAt set
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(doc.getType()).isEqualTo(DocumentTypeEnum.IDENTITY_CARD);
        assertThat(doc.getExpireAt()).isEqualTo(expirationDate);
    }

    @Test
    void execute_shouldSaveDocumentExactlyOnce_whenTextractSucceeds() {
        // Given
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSING);
        List<String> ocrLines = List.of("Documento de Identidad");
        LocalDate expirationDate = LocalDate.of(2026, 6, 30);

        ExpirationDateExtractor extractor = mock(ExpirationDateExtractor.class);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(textractClientAdapter.getLines("job-abc")).thenReturn(ocrLines);
        when(generalDocumentClasifier.classify(ocrLines)).thenReturn(DocumentTypeEnum.IDENTITY_CARD);
        when(expirationDateExtractorSelector.selectExtractor(doc)).thenReturn(extractor);
        when(extractor.extract(any(), any(), anyLong())).thenReturn(expirationDate);

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-abc");

        // Then — one and only one save: at the end after everything succeeds
        verify(documentRepository).save(doc);
    }

    // -------------------------------------------------------------------------
    // execute() — idempotency (already PROCESSED)
    // -------------------------------------------------------------------------

    @Test
    void execute_shouldSkipProcessing_whenDocumentIsAlreadyProcessed() {
        // Given — a document that was already processed (e.g. duplicate SQS delivery).
        // The service must return early without hitting Textract again.
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSED);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-dup");

        // Then — no OCR call and no save: the document is already in its final state
        verify(textractClientAdapter, never()).getLines(any());
        verify(documentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // execute() — document not found
    // -------------------------------------------------------------------------

    @Test
    void execute_shouldThrowIllegalStateException_whenDocumentNotFound() {
        // Given — the documentId does not exist in the database.
        // This is a programming error (the SQS message references a ghost document),
        // so throwing IllegalStateException is appropriate.
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                processTextractResultService.execute(new DocumentId(docId), "job-xyz"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(docId.toString());

        verify(documentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // execute() — failure recovery
    // -------------------------------------------------------------------------

    @Test
    void execute_shouldMarkDocumentAsFailed_whenTextractClientThrowsException() {
        // Given — the Textract client fails (e.g. job expired, transient AWS error).
        // The service must catch the exception, mark the document FAILED, and persist
        // that status so the user knows processing failed and the document won't retry.
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSING);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(textractClientAdapter.getLines(any()))
                .thenThrow(new RuntimeException("Textract job expired"));

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-fail");

        // Then — the document must be saved with FAILED status, not bubble up the error
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(documentRepository).save(doc);
    }

    @Test
    void execute_shouldMarkDocumentAsFailed_whenExpirationDateExtractorThrowsException() {
        // Given — classification succeeds but the extractor crashes (e.g. unexpected
        // OCR layout). Same recovery path: mark FAILED and save.
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSING);
        List<String> ocrLines = List.of("Documento de Identidad");

        ExpirationDateExtractor extractor = mock(ExpirationDateExtractor.class);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(textractClientAdapter.getLines(any())).thenReturn(ocrLines);
        when(generalDocumentClasifier.classify(ocrLines)).thenReturn(DocumentTypeEnum.IDENTITY_CARD);
        when(expirationDateExtractorSelector.selectExtractor(doc)).thenReturn(extractor);
        when(extractor.extract(any(), any(), anyLong()))
                .thenThrow(new RuntimeException("Unexpected OCR layout"));

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-bad-ocr");

        // Then
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(documentRepository).save(doc);
    }

    // -------------------------------------------------------------------------
    // execute() — expiration date can be null
    // -------------------------------------------------------------------------

    @Test
    void execute_shouldMarkDocumentAsProcessed_evenWhenExtractorReturnsNull() {
        // Given — the extractor returns null when the OCR text doesn't contain a
        // recognizable expiration date. This is a valid outcome (not a failure):
        // the document is still marked PROCESSED and the user is prompted manually.
        UUID docId = UUID.randomUUID();
        DocumentModel doc = buildDocument(docId, DocumentStatus.PROCESSING);
        List<String> ocrLines = List.of("PASAPORTE/");

        ExpirationDateExtractor extractor = mock(ExpirationDateExtractor.class);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(textractClientAdapter.getLines(any())).thenReturn(ocrLines);
        when(generalDocumentClasifier.classify(ocrLines)).thenReturn(DocumentTypeEnum.PASSPORT);
        when(expirationDateExtractorSelector.selectExtractor(doc)).thenReturn(extractor);
        when(extractor.extract(any(), any(), anyLong())).thenReturn(null); // no date found

        // When
        processTextractResultService.execute(new DocumentId(docId), "job-no-date");

        // Then — still PROCESSED, expireAt stays null (the user will fill it in manually)
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(doc.getExpireAt()).isNull();
        verify(documentRepository).save(doc);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // UserModel.id is @GeneratedValue with no setter — we mock it so getId() returns 1L.
    // lenient() silences UnnecessaryStubbingException for tests that return early
    // before the service ever calls document.getUser().getId() (e.g. already-PROCESSED,
    // textract failure path).
    private DocumentModel buildDocument(UUID pDocId, DocumentStatus pStatus) {
        UserModel user = mock(UserModel.class);
        lenient().when(user.getId()).thenReturn(1L);

        DocumentModel doc = new DocumentModel();
        doc.setId(pDocId);
        doc.setUser(user);
        doc.markProcessing(); // sets status to PROCESSING as the baseline

        if (pStatus == DocumentStatus.PROCESSED) {
            doc.markProcessed();
        } else if (pStatus == DocumentStatus.FAILED) {
            doc.markFailed();
        }

        return doc;
    }
}
