package com.miguel_damasco.DoSafe.alert.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.Answers;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;
import com.miguel_damasco.DoSafe.alert.repository.AlertRepository;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.email.service.EmailService;
import com.miguel_damasco.DoSafe.user.domain.UserModel;

@ExtendWith(MockitoExtension.class)
// buildMockAlert() stubs alert.getId() which not every test exercises — lenient
// avoids UnnecessaryStubbingException without restructuring the shared helper.
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private EmailService emailService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private AlertService alertService;

    // @Value fields are not injected by @InjectMocks — we set them manually.
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "expirationThresholdDays", 30);
    }

    // -------------------------------------------------------------------------
    // checkExpiringDocuments()
    // -------------------------------------------------------------------------

    @Test
    void checkExpiringDocuments_shouldCreateAlertsAndSendEmails_whenExpiringDocumentsFound() {
        // Given — one expiring document found, one pending alert already in DB
        DocumentModel expiringDocument = buildMockDocument();
        when(documentRepository.findExpiringWithoutAlert(any(LocalDate.class), any(DocumentStatus.class)))
                .thenReturn(List.of(expiringDocument));

        AlertModel pendingAlert = buildMockAlert();
        when(alertRepository.findAllBySentAtIsNull()).thenReturn(List.of(pendingAlert));

        // emailService.send() must return a CompletableFuture so .thenRun() doesn't NPE.
        when(emailService.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        alertService.checkExpiringDocuments();

        // Then — alert created for the new document, email sent for the pending alert
        verify(alertRepository).saveAll(any());
        verify(emailService).send(anyString(), anyString(), anyString());

        // thenRun() executes synchronously on an already-completed future —
        // so markSent() and save() are called before this verify runs.
        verify(pendingAlert).markSent();
        verify(alertRepository).save(pendingAlert);
    }

    @Test
    void checkExpiringDocuments_shouldSkipAlertCreation_whenNoExpiringDocumentsFound() {
        // Given — no new expiring documents, but one pending alert exists
        when(documentRepository.findExpiringWithoutAlert(any(LocalDate.class), any(DocumentStatus.class)))
                .thenReturn(Collections.emptyList());

        AlertModel pendingAlert = buildMockAlert();
        when(alertRepository.findAllBySentAtIsNull()).thenReturn(List.of(pendingAlert));

        when(emailService.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        alertService.checkExpiringDocuments();

        // Then — no new alerts created, but the pending one is still dispatched
        verify(alertRepository, never()).saveAll(any());
        verify(emailService).send(anyString(), anyString(), anyString());
    }

    @Test
    void checkExpiringDocuments_shouldNeverSendEmail_whenNoPendingAlertsExist() {
        // Given — no new documents, no pending alerts
        when(documentRepository.findExpiringWithoutAlert(any(LocalDate.class), any(DocumentStatus.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.findAllBySentAtIsNull()).thenReturn(Collections.emptyList());

        // When
        alertService.checkExpiringDocuments();

        // Then
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void checkExpiringDocuments_shouldNotMarkAlertAsSent_whenEmailFails() {
        // Given — one pending alert, but email sending throws
        when(documentRepository.findExpiringWithoutAlert(any(LocalDate.class), any(DocumentStatus.class)))
                .thenReturn(Collections.emptyList());

        AlertModel pendingAlert = buildMockAlert();
        when(alertRepository.findAllBySentAtIsNull()).thenReturn(List.of(pendingAlert));

        // Simulate an email provider failure — the future completes exceptionally.
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("SES unavailable"));
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        // When — should not throw, exceptionally() handles the error
        alertService.checkExpiringDocuments();

        // Then — alert stays with sentAt = null for retry on next run
        verify(pendingAlert, never()).markSent();
        verify(alertRepository, never()).save(pendingAlert);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Builds a fully mocked AlertModel with a user and document chain ready for
    // buildSubject() and buildBody() calls inside AlertService.
    private AlertModel buildMockAlert() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("user@email.com");
        when(user.getUsername()).thenReturn("miguel");

        DocumentModel document = mock(DocumentModel.class);
        when(document.getType()).thenReturn(DocumentTypeEnum.IDENTITY_CARD);
        when(document.getExpireAt()).thenReturn(LocalDate.now().plusDays(10));

        AlertModel alert = mock(AlertModel.class);
        when(alert.getUser()).thenReturn(user);
        when(alert.getDocument()).thenReturn(document);
        when(alert.getId()).thenReturn("alert-id-1");

        return alert;
    }

    // Builds a mocked DocumentModel used as input to buildAlert() inside AlertService.
    private DocumentModel buildMockDocument() {
        UserModel user = mock(UserModel.class);

        DocumentModel document = mock(DocumentModel.class);
        when(document.getUser()).thenReturn(user);

        return document;
    }
}
