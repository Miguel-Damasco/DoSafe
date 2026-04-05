package com.miguel_damasco.DoSafe.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentPageResponseDTO;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DocumentQueryService documentQueryService;

    // -------------------------------------------------------------------------
    // listByUser() — happy path
    // -------------------------------------------------------------------------

    @Test
    void listByUser_shouldReturnPageDTO_whenUserHasDocuments() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);

        DocumentModel doc1 = buildDocument(DocumentTypeEnum.IDENTITY_CARD, DocumentStatus.PROCESSED);
        DocumentModel doc2 = buildDocument(DocumentTypeEnum.PASSPORT, DocumentStatus.PROCESSING);
        Page<DocumentModel> page = new PageImpl<>(List.of(doc1, doc2));

        when(documentRepository.findByUser(eq(user), any(Pageable.class))).thenReturn(page);

        // When
        DocumentPageResponseDTO result = documentQueryService.listByUser("miguel", 0, 10);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.last()).isTrue();
    }

    @Test
    void listByUser_shouldReturnEmptyPage_whenUserHasNoDocuments() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(Page.empty());

        // When
        DocumentPageResponseDTO result = documentQueryService.listByUser("miguel", 0, 10);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // listByUser() — content mapping
    // -------------------------------------------------------------------------

    @Test
    void listByUser_shouldMapDocumentFieldsCorrectly() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);

        DocumentModel doc = buildDocument(DocumentTypeEnum.IDENTITY_CARD, DocumentStatus.PROCESSED);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc)));

        // When
        DocumentPageResponseDTO result = documentQueryService.listByUser("miguel", 0, 10);

        // Then — verify the mapper correctly translated the domain model to the DTO
        var summary = result.content().get(0);
        assertThat(summary.id()).isEqualTo(doc.getId().toString());
        assertThat(summary.type()).isEqualTo("IDENTITY_CARD");
        assertThat(summary.status()).isEqualTo("PROCESSED");
        assertThat(summary.originalFilename()).isEqualTo("document.jpg");
    }

    @Test
    void listByUser_shouldMapNullTypeCorrectly_whenDocumentIsStillProcessing() {
        // Given — a document with null type represents a document where OCR hasn't
        // completed yet (status = PROCESSING). The mapper must not throw NPE.
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);

        DocumentModel doc = buildDocument(null, DocumentStatus.PROCESSING);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc)));

        // When
        DocumentPageResponseDTO result = documentQueryService.listByUser("miguel", 0, 10);

        // Then — type must be null in the DTO, not throw NullPointerException
        assertThat(result.content().get(0).type()).isNull();
        assertThat(result.content().get(0).status()).isEqualTo("PROCESSING");
    }

    // -------------------------------------------------------------------------
    // listByUser() — size clamping
    // -------------------------------------------------------------------------

    @Test
    void listByUser_shouldClampSizeToMax_whenSizeExceedsLimit() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(Page.empty());

        // ArgumentCaptor captures the Pageable passed to the repository so we can
        // assert on its page size without exposing internal implementation details.
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When — client requests 999 documents per page (above the MAX_PAGE_SIZE of 50)
        documentQueryService.listByUser("miguel", 0, 999);

        // Then — the repository must have received a Pageable with size = 50, not 999
        verify(documentRepository).findByUser(eq(user), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void listByUser_shouldClampSizeToOne_whenSizeIsZeroOrNegative() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(Page.empty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When — client sends size=0 (invalid)
        documentQueryService.listByUser("miguel", 0, 0);

        // Then — must be clamped to 1, never 0 (PageRequest throws on size < 1)
        verify(documentRepository).findByUser(eq(user), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void listByUser_shouldNotClampSize_whenSizeIsWithinBounds() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(Page.empty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When — client requests 25, which is within [1, 50]
        documentQueryService.listByUser("miguel", 0, 25);

        // Then — size must be passed through unchanged
        verify(documentRepository).findByUser(eq(user), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
    }

    // -------------------------------------------------------------------------
    // listByUser() — sort order
    // -------------------------------------------------------------------------

    @Test
    void listByUser_shouldAlwaysSortByCreatedAtDescending() {
        // Given
        UserModel user = buildUser("miguel");
        when(userService.findUserByUsername("miguel")).thenReturn(user);
        when(documentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(Page.empty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When
        documentQueryService.listByUser("miguel", 0, 10);

        // Then — newest documents must always appear first, regardless of what the
        // caller requests. This is a contract, not a configurable option.
        verify(documentRepository).findByUser(eq(user), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.isDescending()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserModel buildUser(String pUsername) {
        UserModel user = new UserModel();
        user.setUsername(pUsername);
        return user;
    }

    // Builds a DocumentModel with the given type and status.
    // pType can be null to simulate a document still being processed by Textract.
    private DocumentModel buildDocument(DocumentTypeEnum pType, DocumentStatus pStatus) {
        DocumentModel doc = new DocumentModel();
        doc.setId(UUID.randomUUID());
        doc.setDocumentType(pType);
        doc.markProcessing(); // sets status to PROCESSING first
        if (pStatus == DocumentStatus.PROCESSED) {
            doc.markProcessed();
        } else if (pStatus == DocumentStatus.FAILED) {
            doc.markFailed();
        }
        doc.setOriginalFilename("document.jpg");
        doc.setCreatedAt(Instant.now());
        return doc;
    }
}
