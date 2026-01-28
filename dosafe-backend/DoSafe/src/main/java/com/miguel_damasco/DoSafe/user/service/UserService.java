package com.miguel_damasco.DoSafe.user.service;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.user.domain.RoleEnum;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.dto.request.RegisterRequestDTO;
import com.miguel_damasco.DoSafe.user.dto.response.RegisterResponseDTO;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository pUserRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = pUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserModel findUserById(long pId) {

        return this.userRepository.findById(pId)
                                            .orElseThrow(() -> 
                                            new UsernameNotFoundException("Username not found!"));

    }

    public void checkUserExists(String pUsername) {

        UserModel user = this.userRepository.findByUsername(pUsername);

        if(user != null) {
            throw new RuntimeException("Userename already exists!");
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

        try {

            UserModel userSaved = this.userRepository.save(newUser);

            return new RegisterResponseDTO(userSaved.getId(), userSaved.getUsername());

        } catch (DataIntegrityViolationException e) {

            throw new RuntimeException("User already exists!");
        }
    }
}
