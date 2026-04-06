package com.miguel_damasco.DoSafe.document.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.mapper.DocumentResponseMapper;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentPageResponseDTO;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.storage.DocumentStorage;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentQueryService {

    // Maximum number of documents the client can request in a single page.
    // Prevents a caller from requesting thousands of rows in one shot.
    private static final int MAX_PAGE_SIZE = 50;

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final UserService userService;

    @Value("${document.download-url.ttl-minutes:15}")
    private int downloadUrlTtlMinutes;

    // Returns a paginated, newest-first list of documents owned by pUsername.
    // Each document in the response includes a presigned S3 URL valid for
    // downloadUrlTtlMinutes so the user can view or download their file directly.
    //
    // pPage is zero-based (page 0 = first page).
    // pSize is clamped to [1, MAX_PAGE_SIZE] so the client cannot request an
    // arbitrarily large result set.
    public DocumentPageResponseDTO listByUser(String pUsername, int pPage, int pSize) {

        // Clamp the requested size to a safe range: minimum 1, maximum MAX_PAGE_SIZE.
        // Math.max ensures size is never 0 or negative (PageRequest throws on size < 1).
        // Math.min ensures no caller can request an arbitrarily large result set.
        int safeSize = Math.min(Math.max(pSize, 1), MAX_PAGE_SIZE);

        log.debug("Listing documents username={} page={} size={}", pUsername, pPage, safeSize);

        // Resolve the authenticated username to the full UserModel entity so we can
        // pass it to the repository as the owner filter.
        UserModel user = userService.findUserByUsername(pUsername);

        // PageRequest encapsulates the page number, size, and sort order into a single
        // object that Spring Data JPA translates into SQL LIMIT / OFFSET / ORDER BY.
        Pageable pageable = PageRequest.of(pPage, safeSize, Sort.by("createdAt").descending());

        Page<DocumentModel> page = documentRepository.findByUser(user, pageable);

        log.debug("Found {} documents (total={}) for username={}", page.getNumberOfElements(), page.getTotalElements(), pUsername);

        // Build the TTL Duration once and reuse it across all documents in the page.
        Duration ttl = Duration.ofMinutes(downloadUrlTtlMinutes);

        // toPageDto maps each DocumentModel to its DTO. The second argument is a
        // function (lambda) that the mapper calls per document to get the download URL.
        return DocumentResponseMapper.toPageDto(page, doc -> resolveDownloadUrl(doc, ttl));
    }

    // Returns null while the document is still PROCESSING — s3Key is not set until
    // the upload to S3 completes. The client should poll until status = PROCESSED.
    private String resolveDownloadUrl(DocumentModel pDoc, Duration pTtl) {
        if (pDoc.getS3Key() == null) {
            return null;
        }
        return documentStorage.generateDownloadUrl(pDoc.getS3Key(), pTtl).toString();
    }
}
