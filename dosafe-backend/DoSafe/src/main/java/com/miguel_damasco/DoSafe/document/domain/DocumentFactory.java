package com.miguel_damasco.DoSafe.document.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

public class DocumentFactory {
    
    public static DocumentModel createPassport(UserModel pUser, String pOriginalFilename, LocalDate pDate) {

        DocumentModel document = new DocumentModel();
        document.setDocumentType(DocumentTypeEnum.PASSPORT);
        document.setUser(pUser);
        document.setCreatedAt(Instant.now());
        document.setExpireAt(pDate);
        document.setOriginalFilename(pOriginalFilename);

        return document;

    }
}
