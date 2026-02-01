package com.miguel_damasco.DoSafe.document.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Entity
@Table(name = "documents")
public class DocumentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentTypeEnum type;

    private Instant createdAt;

    private LocalDate expireAt;

    private String s3Key;

    private String originalFilename;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private UserModel user;

    public void setId(UUID pId) {
        this.id = pId;
    }

    public void setDocumentType(DocumentTypeEnum pType) {
        this.type = pType;
    }

    public void setCreatedAt(Instant pInstant) {
        this.createdAt = pInstant;
    }

    public void setExpireAt(LocalDate pDate) {
        this.expireAt = pDate;
    }

    public void setKey(String pKey) {
        this.s3Key = pKey;
    }

    public void setOriginalFilename(String pOriginalFilename) {
        this.originalFilename = pOriginalFilename;
    }

    public void setUser(UserModel pUser) {
        this.user = pUser;
    }

}
