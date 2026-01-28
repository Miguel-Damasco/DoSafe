package com.miguel_damasco.DoSafe.security.refresh;

import java.time.Instant;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Entity
@Table(name = "refresh_token")
public class RefreshTokenModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(optional = false)
    private UserModel user;

    private Instant expiresAt;

    private boolean revoked;

    public void setToken(String pToken) {
        this.token = pToken;
    }

    public void setUser(UserModel pUser) {
        this.user = pUser;
    }

    public void setExpiresAt(Instant pExpires) {
        this.expiresAt = pExpires;
    }

    public void setRevokedTrue() {
        this.revoked = true;
    }
}
