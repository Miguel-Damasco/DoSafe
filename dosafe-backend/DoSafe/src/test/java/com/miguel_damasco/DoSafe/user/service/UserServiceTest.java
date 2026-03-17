package com.miguel_damasco.DoSafe.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.miguel_damasco.DoSafe.common.exception.UserAlreadyExistsException;
import com.miguel_damasco.DoSafe.common.exception.UserNotFoundException;
import com.miguel_damasco.DoSafe.security.jwt.JwtUtil;
import com.miguel_damasco.DoSafe.security.refresh.RefreshTokenModel;
import com.miguel_damasco.DoSafe.security.refresh.RefreshTokenService;
import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.dto.request.LoginRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.request.RegisterRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.response.LoginResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

// @ExtendWith activates Mockito. Without this, @Mock and @InjectMocks do nothing.
// No Spring context is loaded — this test runs in milliseconds.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // @Mock creates a fake implementation of each dependency.
    // None of them execute real code — we control what they return in each test.
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    // @InjectMocks creates a real UserService and injects all @Mock fields into it.
    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Test
    void register_shouldReturnResponseDTO_whenUserDoesNotExist() {
        // Given
        RegisterRequestDTO request = new RegisterRequestDTO("miguel", "password123", "miguel@email.com");

        // No existing user with that username
        when(userRepository.findByUsername("miguel")).thenReturn(null);

        // Simulate BCrypt encoding
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // Simulate DB save — returns a mocked user with id=1.
        // We mock UserModel here because id is @GeneratedValue and has no setter.
        UserModel savedUser = buildUserWithId(1L, "miguel");
        when(userRepository.save(any(UserModel.class))).thenReturn(savedUser);

        // When
        RegisterResponseDTO result = userService.register(request, RoleEnum.USER);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("miguel");
    }

    @Test
    void register_shouldThrowUserAlreadyExistsException_whenUsernameIsTaken() {
        // Given — a user with that username already exists in the DB
        RegisterRequestDTO request = new RegisterRequestDTO("miguel", "password123", "miguel@email.com");
        when(userRepository.findByUsername("miguel")).thenReturn(buildUser("miguel"));

        // When / Then
        assertThatThrownBy(() -> userService.register(request, RoleEnum.USER))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_shouldThrowUserAlreadyExistsException_whenDatabaseThrowsDataIntegrityViolation() {
        // Given — checkUserExists passes (returns null), but the DB save throws
        // a DataIntegrityViolationException (e.g. concurrent insert of the same username).
        RegisterRequestDTO request = new RegisterRequestDTO("miguel", "password123", "miguel@email.com");
        when(userRepository.findByUsername("miguel")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserModel.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        // When / Then
        assertThatThrownBy(() -> userService.register(request, RoleEnum.USER))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findUserById()
    // -------------------------------------------------------------------------

    @Test
    void findUserById_shouldReturnUser_whenUserExists() {
        // Given
        UserModel user = buildUserWithId(1L, "miguel");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        UserModel result = userService.findUserById(1L);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("miguel");
    }

    @Test
    void findUserById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        // Given — no user with that id in the DB
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.findUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        // Given
        LoginRequestDTO request = new LoginRequestDTO("miguel", "password123");

        // Simulate Spring Security authentication — returns an Authentication object
        Authentication auth = new UsernamePasswordAuthenticationToken("miguel", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        // Simulate finding the user after authentication
        UserModel user = buildUser("miguel");
        when(userRepository.findByUsername("miguel")).thenReturn(user);

        // Simulate JWT generation
        when(jwtUtil.generateToken("miguel")).thenReturn("jwt-token");

        // Simulate refresh token creation
        RefreshTokenModel refreshToken = new RefreshTokenModel();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);
        when(refreshTokenService.create(user)).thenReturn(refreshToken);

        // When
        LoginResponseDTO result = userService.login(request);

        // Then
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Builds a UserModel without an id — for tests that don't need to assert on getId().
    private UserModel buildUser(String pUsername) {
        UserModel user = new UserModel();
        user.setUsername(pUsername);
        user.setPassword("encodedPassword");
        user.setEmail(pUsername + "@email.com");
        user.setRole(RoleEnum.USER);
        return user;
    }

    // Builds a mocked UserModel with a stubbed id.
    // Needed because UserModel.id is @GeneratedValue and has no setter.
    private UserModel buildUserWithId(Long pId, String pUsername) {
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn(pId);
        when(user.getUsername()).thenReturn(pUsername);
        return user;
    }
}
