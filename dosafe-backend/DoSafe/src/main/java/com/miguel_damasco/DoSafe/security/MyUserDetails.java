package com.miguel_damasco.DoSafe.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.miguel_damasco.DoSafe.user.domain.UserModel;

public class MyUserDetails implements UserDetails {

    private final UserModel user;

    public MyUserDetails(UserModel pUserModel) {
        this.user = pUserModel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public @Nullable String getPassword() {
        
       return this.user.getPassword();
    }

    @Override
    public String getUsername() {
       
       return this.user.getUsername();
    }
    
}
