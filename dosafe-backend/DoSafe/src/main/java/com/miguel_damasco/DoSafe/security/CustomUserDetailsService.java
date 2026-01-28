package com.miguel_damasco.DoSafe.security;

import java.util.Optional;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository pUserRepository) {
        this.userRepository = pUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String pIdentifier) throws UsernameNotFoundException {
        
        Optional<UserModel> user = Optional.of(this.userRepository.findByUsername(pIdentifier))
                                            .or(() -> Optional.of(this.userRepository.findByEmail(pIdentifier)));

        if(user.isPresent()) {
            return new MyUserDetails(user.get());
        }

        throw new UsernameNotFoundException("User not found!");
    }
    
}
