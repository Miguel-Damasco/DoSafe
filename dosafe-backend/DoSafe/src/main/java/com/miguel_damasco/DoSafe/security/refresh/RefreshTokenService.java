package com.miguel_damasco.DoSafe.security.refresh;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

@Service
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository pRefreshTokenRepository) {
        this.refreshTokenRepository = pRefreshTokenRepository;
    }   

    public RefreshTokenModel create(UserModel pUser) {
        
        RefreshTokenModel myToken = new RefreshTokenModel();

        myToken.setToken(UUID.randomUUID().toString());
        myToken.setUser(pUser);
        myToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        return refreshTokenRepository.save(myToken);
    }

    public RefreshTokenModel validate(String pToken) {

        RefreshTokenModel myToken = refreshTokenRepository
                                            .findByToken(pToken).orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if(myToken.isRevoked() || myToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        return myToken;
    }

    public void revoke(RefreshTokenModel pToken) {
        pToken.setRevokedTrue();
        refreshTokenRepository.save(pToken);
    }
}
