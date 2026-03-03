package com.miguel_damasco.DoSafe.document.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

public class DocumentFactory {
    
    public static DocumentModel create(UserModel pUser, String pOriginalFilename, LocalDate pDate) {

        DocumentModel document = new DocumentModel();
        document.setUser(pUser);
        document.setCreatedAt(Instant.now());
        document.setExpireAt(pDate);
        document.setDocumentType(DocumentTypeEnum.OTHER);
        document.setOriginalFilename(pOriginalFilename);
        document.markProcessing();

        return document;

    }
}
