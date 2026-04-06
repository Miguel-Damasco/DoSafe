package com.miguel_damasco.DoSafe.document.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.common.exception.DocumentNotFoundException;
import com.miguel_damasco.DoSafe.common.exception.DocumentOwnershipException;
import com.miguel_damasco.DoSafe.common.exception.InvalidExpirationDateException;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentSummaryResponseDTO;
import com.miguel_damasco.DoSafe.document.dto.mapper.DocumentResponseMapper;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentUpdateService {

    private final DocumentRepository documentRepository;
    private final UserService userService;

    public DocumentSummaryResponseDTO updateExpirationDate(String pUsername, String pDocumentId, LocalDate pExpireAt) {

        UserModel user = userService.findUserByUsername(pUsername);

        UUID id = UUID.fromString(pDocumentId);

        DocumentModel document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(pDocumentId));

        // Security check: the document must belong to the authenticated user.
        // Without this, any authenticated user could update any document by guessing a UUID.
        if (!document.getUser().getId().equals(user.getId())) {
            throw new DocumentOwnershipException();
        }

        if (pExpireAt.isBefore(LocalDate.now())) {
            throw new InvalidExpirationDateException();
        }

        document.setExpireAt(pExpireAt);
        documentRepository.save(document);

        log.info("Expiration date updated documentId={} username={} newDate={}", pDocumentId, pUsername, pExpireAt);

        return DocumentResponseMapper.toSummaryDto(document, document.getS3Key() != null ? document.getS3Key() : null);
    }
}
