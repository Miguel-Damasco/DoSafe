package com.miguel_damasco.DoSafe.document.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.common.exception.DocumentNotFoundException;
import com.miguel_damasco.DoSafe.common.exception.DocumentOwnershipException;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.storage.DocumentStorage;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentDeleteService {

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final UserService userService;

    public void delete(String pUsername, String pDocumentId) {

        UserModel user = userService.findUserByUsername(pUsername);

        UUID id = UUID.fromString(pDocumentId);

        DocumentModel document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(pDocumentId));

        // Security check: the document must belong to the authenticated user.
        // Without this, any authenticated user could delete any document by guessing a UUID.
        if (!document.getUser().getId().equals(user.getId())) {
            throw new DocumentOwnershipException();
        }

        // Delete from S3 only if the file was already uploaded.
        // A document with no s3Key is one that failed before the upload step.
        if (document.getS3Key() != null) {
            documentStorage.delete(document.getS3Key());
            log.info("S3 object deleted documentId={} s3Key={}", pDocumentId, document.getS3Key());
        }

        documentRepository.delete(document);

        log.info("Document deleted documentId={} username={}", pDocumentId, pUsername);
    }
}
