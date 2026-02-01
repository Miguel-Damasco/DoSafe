package com.miguel_damasco.DoSafe.user.controller;

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

@RestController
@RequestMapping(path = "/authentication")
public class AuthenticationController {
    
    private final UserService userService;

    public AuthenticationController(UserService pUserService) {
        this.userService = pUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO pRequest) {

        LoginResponseDTO response = this.userService.login(pRequest);

        return ResponseEntity.ok().body(ApiResponses.success(response, 200, "Login successfully!"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> register(@RequestBody RegisterRequestDTO pRequest) {

        RegisterResponseDTO response = this.userService.register(pRequest, RoleEnum.USER);

        return ResponseEntity.ok().body(ApiResponses.success(response, 200, "User register successfully!"));
    }
}
