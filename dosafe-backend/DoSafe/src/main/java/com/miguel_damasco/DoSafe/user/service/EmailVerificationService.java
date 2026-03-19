package com.miguel_damasco.DoSafe.user.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.common.exception.InvalidVerificationTokenException;
import com.miguel_damasco.DoSafe.email.service.EmailService;
import com.miguel_damasco.DoSafe.user.domain.EmailVerificationToken;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.repository.EmailVerificationTokenRepository;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final long TOKEN_VALIDITY_HOURS = 24;

    // Called after registration — creates a token (sync) and fires the email (async).
    public void sendVerificationEmail(UserModel pUser) {

        EmailVerificationToken token = createToken(pUser);

        String link = baseUrl + "/authentication/verify-email?token=" + token.getToken();

        emailService.send(
                pUser.getEmail(),
                "Verify your DoSafe email",
                buildBody(pUser.getUsername(), link)
        );

        log.info("Verification email dispatched userId={}", pUser.getId());
    }

    // Validates the token and marks the user's email as verified.
    public void verifyToken(String pToken) {

        EmailVerificationToken token = tokenRepository.findByToken(pToken)
                .orElseThrow(InvalidVerificationTokenException::new);

        if (token.isUsed() || token.isExpired()) {
            throw new InvalidVerificationTokenException();
        }

        token.markUsed();
        tokenRepository.save(token);

        UserModel user = token.getUser();
        user.verifyEmail();
        userRepository.save(user);

        log.info("Email verified userId={}", user.getId());
    }

    // Invalidates any existing unused tokens and sends a fresh one.
    // Returns quietly if the email is not found or already verified — this avoids
    // leaking information about whether a given email is registered (email enumeration).
    public void resendVerificationEmail(String pEmail) {

        UserModel user = userRepository.findByEmail(pEmail);

        if (user == null || user.isEmailVerified()) {
            log.info("Resend verification skipped email={} (not found or already verified)", pEmail);
            return;
        }

        // Invalidate all existing unused tokens so only the new one is valid.
        List<EmailVerificationToken> existingTokens = tokenRepository.findAllByUserAndUsedAtIsNull(user);
        tokenRepository.deleteAll(existingTokens);

        sendVerificationEmail(user);

        log.info("Verification email resent userId={}", user.getId());
    }

    private EmailVerificationToken createToken(UserModel pUser) {

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(pUser);
        token.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS));

        return tokenRepository.save(token);
    }

    private String buildBody(String pUsername, String pLink) {
        return String.format("""
                Hi %s,

                Please verify your email address by clicking the link below:

                %s

                This link expires in 24 hours.

                If you didn't create a DoSafe account, you can safely ignore this email.

                DoSafe Team
                """, pUsername, pLink);
    }
}
