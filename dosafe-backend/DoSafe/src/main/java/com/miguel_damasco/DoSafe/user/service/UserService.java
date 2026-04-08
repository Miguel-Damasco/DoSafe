package com.miguel_damasco.DoSafe.user.service;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.common.exception.UserAlreadyExistsException;
import com.miguel_damasco.DoSafe.common.exception.UserNotFoundException;

import com.miguel_damasco.DoSafe.security.jwt.JwtUtil;
import com.miguel_damasco.DoSafe.security.refresh.RefreshTokenModel;
import com.miguel_damasco.DoSafe.security.refresh.RefreshTokenService;
import com.miguel_damasco.DoSafe.security.refresh.dto.RefreshRequestDTO;
import com.miguel_damasco.DoSafe.security.refresh.dto.RefreshResponseDTO;
import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.dto.request.LoginRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.request.RegisterRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.response.LoginResponseDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final RefreshTokenService refreshTokenService;

    private final EmailVerificationService emailVerificationService;

    public UserService(UserRepository pUserRepository, PasswordEncoder passwordEncoder,
         JwtUtil pJwtUtil,
         AuthenticationManager pAuthenticationManager,
         RefreshTokenService pRefreshTokenService,
         EmailVerificationService pEmailVerificationService) {
        this.userRepository = pUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = pJwtUtil;
        this.authenticationManager = pAuthenticationManager;
        this.refreshTokenService = pRefreshTokenService;
        this.emailVerificationService = pEmailVerificationService;
    }

    public UserModel findUserById(long pId) {

        return this.userRepository.findById(pId)
                                            .orElseThrow(() ->
                                            new UserNotFoundException(String.valueOf(pId)));

    }

    public UserModel findUserByUsername(String pUsername) {

        return this.userRepository.findByUsername(pUsername);
    }

    public void checkUserExists(String pUsername) {

        UserModel user = this.userRepository.findByUsername(pUsername);

        if(user != null) {
            throw new UserAlreadyExistsException(pUsername);
        }
    }

    public RegisterResponseDTO registerRegularUser(RegisterRequestDTO pRequest) {

        return register(pRequest, RoleEnum.USER);
    }

    public RegisterResponseDTO registerAdminUser(RegisterRequestDTO pRequest) {

        return register(pRequest, RoleEnum.ADMIN);
    }


    public RegisterResponseDTO register(RegisterRequestDTO pRequest, RoleEnum pRole) {

        checkUserExists(pRequest.username());

        UserModel newUser = new UserModel();

        newUser.setEmail(pRequest.email());
        newUser.setPassword(passwordEncoder.encode(pRequest.password()));
        newUser.setUsername(pRequest.username());
        newUser.setRole(pRole);

        UserModel userSaved;

        try {
            userSaved = this.userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException(pRequest.username());
        }

        // Fire verification email after the user is saved — outside the catch block
        // so any DB errors here don't get misclassified as UserAlreadyExistsException.
        // Email is sent asynchronously; registration response is not delayed.
        emailVerificationService.sendVerificationEmail(userSaved);

        return new RegisterResponseDTO(userSaved.getId(), userSaved.getUsername());
    }


    public LoginResponseDTO login(LoginRequestDTO pRequest) {

        Authentication authentication = this.authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(pRequest.identifier(), pRequest.password())
        );

        UserModel user = this.userRepository.findByUsername(authentication.getName());

        String token = this.jwtUtil.generateToken(authentication.getName());

        RefreshTokenModel refreshToken = this.refreshTokenService.create(user);

        return new LoginResponseDTO(refreshToken.getToken(), token, user.isEmailVerified(), user.getEmail());
    }

    public RefreshResponseDTO refresh(RefreshRequestDTO pRequest) {

        RefreshTokenModel oldToken = this.refreshTokenService.validate(pRequest.refreshToken());

        this.refreshTokenService.revoke(oldToken);

        RefreshTokenModel newToken = this.refreshTokenService.create(oldToken.getUser());

        String newAccessToken = this.jwtUtil.generateToken(oldToken.getUser().getUsername());

        return new RefreshResponseDTO(newAccessToken, newToken.getToken());
    }

    public void logout(RefreshRequestDTO pRequest) {

        RefreshTokenModel myToken = this.refreshTokenService.validate(pRequest.refreshToken());
        this.refreshTokenService.revoke(myToken);
    }

    public UserModel update(UserModel pUser) {
        return this.userRepository.save(pUser);
    }
}
