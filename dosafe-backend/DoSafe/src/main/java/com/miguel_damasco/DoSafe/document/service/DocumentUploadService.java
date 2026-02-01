package com.miguel_damasco.DoSafe.document.service;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;

import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.document.domain.DocumentFactory;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.dto.mapper.DocumentResponseMapper;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.storage.DocumentStorage;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

@Service
public class DocumentUploadService {
    
    private final DocumentRepository documentRepository;

    private final DocumentStorage documentStorage;

    private final UserService userService;

    public DocumentUploadService(DocumentRepository pDocumentRepository, DocumentStorage pDocumentStorage, UserService pUserService) {
        this.documentRepository = pDocumentRepository;
        this.documentStorage = pDocumentStorage;
        this.userService = pUserService;
    }


    public  DocumentUploadResponseDTO upload(String pUsername, InputStream pContent, long pSize, String pOriginalFilename) {

        UserModel user = this.userService.findUserByUsername(pUsername);

        DocumentModel newDocument = DocumentFactory.createPassport(user, pOriginalFilename, LocalDate.of(2026, Month.APRIL, 20));

        DocumentModel savedDocuemnt = this.documentRepository.save(newDocument);

        String key = this.documentStorage.upload(user.getId(), savedDocuemnt.getId().toString(), pContent, pSize, savedDocuemnt.getType().toString());

        savedDocuemnt.setKey(key);

        this.documentRepository.save(savedDocuemnt);

        return DocumentResponseMapper.toDto(newDocument);

    }

    
}
