package com.miguel_damasco.DoSafe.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;
import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.dto.request.LoginRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.request.RegisterRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.response.LoginResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
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
}
