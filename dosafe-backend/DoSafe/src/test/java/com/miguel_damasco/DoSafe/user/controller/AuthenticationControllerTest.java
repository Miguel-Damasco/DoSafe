package com.miguel_damasco.DoSafe.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miguel_damasco.DoSafe.config.SecurityConfig;

import com.miguel_damasco.DoSafe.common.exception.UserAlreadyExistsException;
import com.miguel_damasco.DoSafe.security.CustomUserDetailsService;
import com.miguel_damasco.DoSafe.security.jwt.JwtUtil;
import com.miguel_damasco.DoSafe.user.dto.response.LoginResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
import com.miguel_damasco.DoSafe.user.service.EmailVerificationService;
import com.miguel_damasco.DoSafe.user.service.UserService;

// @WebMvcTest loads only the web layer: controllers, filters, and JSON serialization.
// It does NOT load repositories, services, or the database — those are mocked.
// This makes it faster than @SpringBootTest and focused on HTTP behavior.
// @Import loads SecurityConfig so that @WebMvcTest uses our custom filter chain
// (with permitAll for /authentication/*) instead of Spring Boot's default one.
@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    // MockMvc simulates HTTP requests without opening a real server port.
    // The request goes through all filters and the controller, but entirely in memory.
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean registers a Mockito mock in the Spring context.
    // Spring injects it wherever the real bean would go.
    // Without this, Spring would try to load UserService with all its dependencies (DB, AWS, etc.)
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    // JwtUtil reads ${jwt.secret} from application.properties — we mock it to avoid
    // needing a real secret key in the test environment.
    // The real JwtAuthenticationFilter is used (not mocked): requests without an
    // Authorization header pass through the filter without any JWT validation,
    // so Spring Security's permitAll() rules apply normally.
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // -------------------------------------------------------------------------
    // POST /authentication/register
    // -------------------------------------------------------------------------

    @Test
    void register_shouldReturn200_whenRequestIsValid() throws Exception {
        // Given — stub the service to return a successful registration
        when(userService.register(any(), any()))
                .thenReturn(new RegisterResponseDTO(1L, "miguel"));

        // When — simulate POST /authentication/register with a valid JSON body
        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "miguel",
                                    "password": "password123",
                                    "email": "miguel@email.com"
                                }
                                """))
                // Then — verify HTTP status and JSON response fields
                // jsonPath navigates the JSON like a tree: $ is the root
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("miguel"))
                .andExpect(jsonPath("$.meta.success").value(true))
                .andExpect(jsonPath("$.meta.statusCode").value(201));
    }

    @Test
    void register_shouldReturn409_whenUsernameIsAlreadyTaken() throws Exception {
        // Given — stub the service to throw UserAlreadyExistsException
        when(userService.register(any(), any()))
                .thenThrow(new UserAlreadyExistsException("miguel"));

        // When
        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "miguel",
                                    "password": "password123",
                                    "email": "miguel@email.com"
                                }
                                """))
                // Then — GlobalExceptionHandler converts the exception to a 409 response
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.meta.success").value(false))
                .andExpect(jsonPath("$.meta.statusCode").value(409));
    }

    // -------------------------------------------------------------------------
    // POST /authentication/login
    // -------------------------------------------------------------------------

    @Test
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        // Given
        when(userService.login(any()))
                .thenReturn(new LoginResponseDTO("refresh-token", "jwt-token", true, "miguel@test.com"));

        // When
        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "identifier": "miguel",
                                    "password": "password123"
                                }
                                """))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.meta.success").value(true));
    }
}
