package com.miguel_damasco.DoSafe.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.miguel_damasco.DoSafe.common.exception.InvalidVerificationTokenException;
import com.miguel_damasco.DoSafe.email.service.EmailService;
import com.miguel_damasco.DoSafe.user.domain.EmailVerificationToken;
import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.repository.EmailVerificationTokenRepository;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    // @Value fields are not injected by @InjectMocks — we set them manually.
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:5173");
    }

    // -------------------------------------------------------------------------
    // verifyToken()
    // -------------------------------------------------------------------------

    @Test
    void verifyToken_shouldMarkUserAsVerified_whenTokenIsValid() {
        // Given
        UserModel user = mock(UserModel.class);

        // Mock the token so we can verify markUsed() and getUser() are called.
        EmailVerificationToken token = mock(EmailVerificationToken.class);
        when(token.isUsed()).thenReturn(false);
        when(token.isExpired()).thenReturn(false);
        when(token.getUser()).thenReturn(user);
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        // When
        emailVerificationService.verifyToken("valid-token");

        // Then — token is marked used, user is marked verified, both are persisted.
        verify(token).markUsed();
        verify(tokenRepository).save(token);
        verify(user).verifyEmail();
        verify(userRepository).save(user);
    }

    @Test
    void verifyToken_shouldThrow_whenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> emailVerificationService.verifyToken("unknown-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void verifyToken_shouldThrow_whenTokenIsExpired() {
        // Given
        EmailVerificationToken expiredToken = mock(EmailVerificationToken.class);
        when(expiredToken.isUsed()).thenReturn(false);
        when(expiredToken.isExpired()).thenReturn(true);
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // When / Then
        assertThatThrownBy(() -> emailVerificationService.verifyToken("expired-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);

        // The user must never be marked as verified for an expired token.
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyToken_shouldThrow_whenTokenIsAlreadyUsed() {
        // Given
        EmailVerificationToken usedToken = mock(EmailVerificationToken.class);
        when(usedToken.isUsed()).thenReturn(true);
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        // When / Then
        assertThatThrownBy(() -> emailVerificationService.verifyToken("used-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // sendVerificationEmail()
    // -------------------------------------------------------------------------

    @Test
    void sendVerificationEmail_shouldCreateTokenAndSendEmailWithVerificationLink() {
        // Given
        UserModel user = buildUser("miguel", "miguel@email.com");

        // tokenRepository.save() returns the token as-is so it keeps the UUID set by createToken().
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        emailVerificationService.sendVerificationEmail(user);

        // Then — email is sent to the user's address with a body containing the verification link.
        verify(emailService).send(
                eq("miguel@email.com"),
                eq("Verify your DoSafe email"),
                contains("http://localhost:5173/verify-email?token=")
        );
    }

    // -------------------------------------------------------------------------
    // resendVerificationEmail()
    // -------------------------------------------------------------------------

    @Test
    void resendVerificationEmail_shouldDoNothing_whenEmailIsNotRegistered() {
        // Given — no user found for that email
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(null);

        // When
        emailVerificationService.resendVerificationEmail("unknown@email.com");

        // Then — no email sent, no tokens touched (email enumeration prevention)
        verify(emailService, never()).send(anyString(), anyString(), anyString());
        verify(tokenRepository, never()).deleteAll(any());
    }

    @Test
    void resendVerificationEmail_shouldDoNothing_whenUserIsAlreadyVerified() {
        // Given — user exists but email is already verified
        UserModel verifiedUser = buildUser("miguel", "miguel@email.com");
        verifiedUser.verifyEmail();
        when(userRepository.findByEmail("miguel@email.com")).thenReturn(verifiedUser);

        // When
        emailVerificationService.resendVerificationEmail("miguel@email.com");

        // Then — no email sent (nothing to verify)
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_shouldDeleteOldTokensAndSendNewEmail_whenUserIsNotVerified() {
        // Given — user exists and email is not verified
        UserModel user = buildUser("miguel", "miguel@email.com");
        when(userRepository.findByEmail("miguel@email.com")).thenReturn(user);

        // Two existing unused tokens that should be invalidated
        EmailVerificationToken oldToken1 = mock(EmailVerificationToken.class);
        EmailVerificationToken oldToken2 = mock(EmailVerificationToken.class);
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user))
                .thenReturn(List.of(oldToken1, oldToken2));

        // tokenRepository.save() returns the new token so sendVerificationEmail() can build the link
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        emailVerificationService.resendVerificationEmail("miguel@email.com");

        // Then — old tokens deleted, fresh email sent
        verify(tokenRepository).deleteAll(List.of(oldToken1, oldToken2));
        verify(emailService).send(
                eq("miguel@email.com"),
                anyString(),
                anyString()
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserModel buildUser(String pUsername, String pEmail) {
        UserModel user = new UserModel();
        user.setUsername(pUsername);
        user.setEmail(pEmail);
        user.setPassword("encoded");
        user.setRole(RoleEnum.USER);
        return user;
    }
}
