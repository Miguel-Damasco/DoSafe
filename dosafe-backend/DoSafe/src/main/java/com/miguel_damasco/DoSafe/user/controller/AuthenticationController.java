package com.miguel_damasco.DoSafe.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;
import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.dto.request.LoginRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.request.RegisterRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.request.ResendVerificationRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.response.LoginResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.VerifyEmailResponseDTO;
import com.miguel_damasco.DoSafe.user.service.EmailVerificationService;
import com.miguel_damasco.DoSafe.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/authentication")
// @Tag — groups both endpoints under "Authentication" in the Swagger UI sidebar.
@Tag(name = "Authentication", description = "User registration and login")
public class AuthenticationController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT access token and a refresh token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful — returns JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO pRequest) {

        log.info("Login attempt identifier={}", pRequest.identifier());

        LoginResponseDTO response = userService.login(pRequest);

        log.info("Login successful identifier={}", pRequest.identifier());

        return ResponseEntity.ok().body(ApiResponses.success(response, 200, "Login successfully!"));
    }

    @Operation(summary = "Register", description = "Creates a new user account with the USER role.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username already taken")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> register(
            @RequestBody RegisterRequestDTO pRequest) {

        log.info("Register attempt username={}", pRequest.username());

        RegisterResponseDTO response = userService.register(pRequest, RoleEnum.USER);

        log.info("Register successful username={}", pRequest.username());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(response, 201, "User registered successfully!"));
    }

    @Operation(summary = "Verify email", description = "Validates a verification token and marks the user's email as verified.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token is invalid, expired, or already used")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerifyEmailResponseDTO>> verifyEmail(
            @RequestParam String token) {

        log.info("Email verification attempt token={}", token);

        emailVerificationService.verifyToken(token);

        log.info("Email verified successfully token={}", token);

        return ResponseEntity.ok()
                .body(ApiResponses.success(null, 200, "Email verified successfully!"));
    }

    @Operation(summary = "Resend verification email", description = "Sends a new verification email. Returns 200 regardless of whether the email exists to prevent email enumeration.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification email sent if the address is registered and unverified")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody ResendVerificationRequestDTO pRequest) {

        log.info("Resend verification requested email={}", pRequest.email());

        emailVerificationService.resendVerificationEmail(pRequest.email());

        return ResponseEntity.ok()
                .body(ApiResponses.success(null, 200, "If your email is registered and unverified, a new verification email has been sent."));
    }
}
