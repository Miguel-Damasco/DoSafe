package com.miguel_damasco.DoSafe.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import org.mockito.InOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.miguel_damasco.DoSafe.common.exception.RateLimitExceededException;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;
import com.miguel_damasco.DoSafe.document.infraestructure.conversion.ConvertedDocument;
import com.miguel_damasco.DoSafe.document.infraestructure.conversion.DocumentConverter;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.storage.DocumentStorage;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentStorage documentStorage;

    @Mock
    private DocumentConverter documentConverter;

    @Mock
    private UserService userService;

    @InjectMocks
    private DocumentUploadService documentUploadService;

    // @Value fields are not injected by @InjectMocks — Mockito doesn't process
    // Spring annotations. Without this setup, perUserDailyLimit and globalDailyLimit
    // would be 0, making every upload immediately throw RateLimitExceededException.
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentUploadService, "perUserDailyLimit", 3);
        ReflectionTestUtils.setField(documentUploadService, "globalDailyLimit", 10);
    }

    // -------------------------------------------------------------------------
    // upload() — happy path
    // -------------------------------------------------------------------------

    @Test
    void upload_shouldReturnResponseDTO_whenUploadIsSuccessful() {
        // Given
        UserModel user = buildUser(1L);
        InputStream content = new ByteArrayInputStream("fake-content".getBytes());
        ConvertedDocument pdf = new ConvertedDocument(content, 100L, "application/pdf");

        when(userService.findUserByUsername("miguel")).thenReturn(user);

        // thenAnswer simulates JPA behavior: entityManager.persist() mutates the entity
        // in-place and assigns the generated id. Without this, getId() returns null
        // and DocumentResponseMapper.toDto() throws NullPointerException.
        when(documentRepository.save(any(DocumentModel.class))).thenAnswer(invocation -> {
            DocumentModel doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID()); // first save: assign generated id
            }
            return doc;
        });

        when(documentConverter.convertToPdf(any(), anyString(), anyString())).thenReturn(pdf);
        when(documentStorage.upload(anyLong(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("users/1/abc123.pdf");

        // When
        DocumentUploadResponseDTO result = documentUploadService.upload(
                "miguel",
                content,
                100L,
                "passport.jpg"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.originalFilename()).isEqualTo("passport.jpg");
        assertThat(result.type()).isEqualTo("OTHER"); // DocumentFactory always starts with OTHER
    }

    @Test
    void upload_shouldSaveDocumentTwice_firstWithoutKeyThenWithKey() {
        // Given
        UserModel user = buildUser(1L);
        InputStream content = new ByteArrayInputStream("fake-content".getBytes());
        ConvertedDocument pdf = new ConvertedDocument(content, 100L, "application/pdf");

        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.save(any(DocumentModel.class))).thenAnswer(invocation -> {
            DocumentModel doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });
        when(documentConverter.convertToPdf(any(), anyString(), anyString())).thenReturn(pdf);
        when(documentStorage.upload(anyLong(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("users/1/abc123.pdf");

        // When
        documentUploadService.upload("miguel", content, 100L, "passport.jpg");

        // Then — verify save() was called exactly twice total
        verify(documentRepository, times(2)).save(any(DocumentModel.class));

        // InOrder verifies that the mocks were called in a specific sequence.
        // This confirms the flow: save (get id) → upload to S3 → save (store key).
        // Without InOrder, verify() only checks that the calls happened, not when.
        InOrder inOrder = org.mockito.Mockito.inOrder(documentRepository, documentStorage);
        inOrder.verify(documentRepository).save(any(DocumentModel.class));
        inOrder.verify(documentStorage).upload(anyLong(), anyString(), any(), anyLong(), anyString());
        inOrder.verify(documentRepository).save(any(DocumentModel.class));
    }

    @Test
    void upload_shouldSetDocumentStatusToProcessing_whenCreated() {
        // Given
        UserModel user = buildUser(1L);
        InputStream content = new ByteArrayInputStream("fake-content".getBytes());
        ConvertedDocument pdf = new ConvertedDocument(content, 100L, "application/pdf");

        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.save(any(DocumentModel.class))).thenAnswer(invocation -> {
            DocumentModel doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });
        when(documentConverter.convertToPdf(any(), anyString(), anyString())).thenReturn(pdf);
        when(documentStorage.upload(anyLong(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("users/1/abc123.pdf");

        // When
        DocumentUploadResponseDTO result = documentUploadService.upload(
                "miguel", content, 100L, "passport.jpg"
        );

        // Then — document starts as PROCESSING, meaning Textract hasn't run yet.
        // The status will be updated to PROCESSED by ProcessTextractResultService later.
        assertThat(result.type()).isEqualTo("OTHER");
        assertThat(result.expireAt()).isNull(); // no expiration date until Textract processes it
    }

    // -------------------------------------------------------------------------
    // checkRateLimits() — per-user limit
    // -------------------------------------------------------------------------

    @Test
    void upload_shouldThrowRateLimitExceededException_whenPerUserLimitIsReached() {
        // Given — user has already uploaded 3 documents today (= the limit)
        UserModel user = buildUser(1L);
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.countByUserAndCreatedAtBetween(any(UserModel.class), any(Instant.class), any(Instant.class)))
                .thenReturn(3L);

        // When / Then — the upload must be rejected before any S3 or DB write
        assertThatThrownBy(() -> documentUploadService.upload(
                "miguel",
                new ByteArrayInputStream("fake".getBytes()),
                4L,
                "passport.jpg"))
                .isInstanceOf(RateLimitExceededException.class);

        // No document must be saved when the limit is already reached
        verify(documentRepository, never()).save(any(DocumentModel.class));
        verify(documentStorage, never()).upload(anyLong(), anyString(), any(), anyLong(), anyString());
    }

    // -------------------------------------------------------------------------
    // checkRateLimits() — global limit
    // -------------------------------------------------------------------------

    @Test
    void upload_shouldThrowRateLimitExceededException_whenGlobalLimitIsReached() {
        // Given — this user has only uploaded 1 document today, but the system-wide
        // limit of 10 has been reached by all users combined
        UserModel user = buildUser(1L);
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.countByUserAndCreatedAtBetween(any(UserModel.class), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(documentRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(10L);

        // When / Then
        assertThatThrownBy(() -> documentUploadService.upload(
                "miguel",
                new ByteArrayInputStream("fake".getBytes()),
                4L,
                "passport.jpg"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(documentRepository, never()).save(any(DocumentModel.class));
        verify(documentStorage, never()).upload(anyLong(), anyString(), any(), anyLong(), anyString());
    }

    // -------------------------------------------------------------------------
    // checkRateLimits() — both limits below threshold
    // -------------------------------------------------------------------------

    @Test
    void upload_shouldProceed_whenBothLimitsAreNotReached() {
        // Given — user has uploaded 1 document today, system has 5 total: both below limits
        UserModel user = buildUser(1L);
        InputStream content = new ByteArrayInputStream("fake-content".getBytes());
        ConvertedDocument pdf = new ConvertedDocument(content, 100L, "application/pdf");

        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.countByUserAndCreatedAtBetween(any(UserModel.class), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(documentRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(5L);
        when(documentRepository.save(any(DocumentModel.class))).thenAnswer(invocation -> {
            DocumentModel doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });
        when(documentConverter.convertToPdf(any(), anyString(), anyString())).thenReturn(pdf);
        when(documentStorage.upload(anyLong(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("users/1/abc123.pdf");

        // When
        DocumentUploadResponseDTO result = documentUploadService.upload(
                "miguel", content, 100L, "passport.jpg");

        // Then — upload went through normally
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // UserModel.id is @GeneratedValue with no setter — we mock it to stub getId().
    private UserModel buildUser(long pId) {
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn(pId);
        return user;
    }
}
