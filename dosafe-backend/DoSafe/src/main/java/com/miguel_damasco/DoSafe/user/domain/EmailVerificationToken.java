package com.miguel_damasco.DoSafe.user.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // ManyToOne is EAGER by default — no LazyInitializationException risk here.
    @ManyToOne(optional = false)
    private UserModel user;

    private Instant expiresAt;

    // null means the token hasn't been used yet.
    private Instant usedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public void setToken(String pToken) {
        this.token = pToken;
    }

    public void setUser(UserModel pUser) {
        this.user = pUser;
    }

    public void setExpiresAt(Instant pExpiresAt) {
        this.expiresAt = pExpiresAt;
    }
}
