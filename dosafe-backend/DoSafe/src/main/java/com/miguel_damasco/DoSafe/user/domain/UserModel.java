package com.miguel_damasco.DoSafe.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "user")
public class UserModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleEnum role;

    public void setUsername(String pUsername) {
        this.username = pUsername;
    }

    public void setPassword(String pPassword) {
        this.password = pPassword;
    }

    public void setEmail(String pEmail) {
        this.password = pEmail;
    }

    public void setRole(RoleEnum pRoleEnum) {
        this.role = pRoleEnum;
    }


}
