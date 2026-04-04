package com.miguel_damasco.DoSafe.document.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.mapper.DocumentResponseMapper;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentPageResponseDTO;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
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
    private final UserService userService;

    // Returns a paginated, newest-first list of documents owned by pUsername.
    //
    // pPage is zero-based (page 0 = first page).
    // pSize is clamped to [1, MAX_PAGE_SIZE] so the client cannot request an
    // arbitrarily large result set.
    public DocumentPageResponseDTO listByUser(String pUsername, int pPage, int pSize) {

        int safeSize = Math.min(Math.max(pSize, 1), MAX_PAGE_SIZE);

        log.debug("Listing documents username={} page={} size={}", pUsername, pPage, safeSize);

        UserModel user = userService.findUserByUsername(pUsername);

        Pageable pageable = PageRequest.of(pPage, safeSize, Sort.by("createdAt").descending());

        Page<DocumentModel> page = documentRepository.findByUser(user, pageable);

        log.debug("Found {} documents (total={}) for username={}", page.getNumberOfElements(), page.getTotalElements(), pUsername);

        return DocumentResponseMapper.toPageDto(page);
    }
}
