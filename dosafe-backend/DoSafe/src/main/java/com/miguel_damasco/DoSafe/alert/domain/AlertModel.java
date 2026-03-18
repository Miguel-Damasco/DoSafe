package com.miguel_damasco.DoSafe.alert.domain;

import java.time.Instant;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.user.domain.UserModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "alerts")
public class AlertModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // The user who will receive the email notification.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private UserModel user;

    // The document whose expiration date triggered this alert.
    // Required to include document details (type, expireAt) in the email body.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_document", nullable = false)
    private DocumentModel document;

    // When this alert record was created (i.e. when the scheduler detected the upcoming expiration).
    @Column(nullable = false)
    private Instant createdAt;

    // Null = email not yet sent. Non-null = email was sent at this timestamp.
    // The scheduler uses this to avoid sending duplicate notifications.
    private Instant sentAt;

    public void markSent() {
        this.sentAt = Instant.now();
    }

    public boolean isSent() {
        return this.sentAt != null;
    }

    public void setUser(UserModel pUser) {
        this.user = pUser;
    }

    public void setDocument(DocumentModel pDocument) {
        this.document = pDocument;
    }

    public void setCreatedAt(Instant pCreatedAt) {
        this.createdAt = pCreatedAt;
    }
}
