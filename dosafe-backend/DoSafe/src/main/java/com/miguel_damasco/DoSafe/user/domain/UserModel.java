package com.miguel_damasco.DoSafe.user.domain;

import java.util.ArrayList;
import java.util.List;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "users")
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentModel> documents = new ArrayList<>();

    public void AddDocument(DocumentModel pDocument) {
        pDocument.setUser(this);
        documents.add(pDocument);
    }

    public void RemoveDocument(DocumentModel pDocument) {
        documents.remove(pDocument);
        pDocument.setUser(null);
    }

    public List<DocumentModel> getDocuments() {
        return List.copyOf(documents);
    }

    public void setUsername(String pUsername) {
        this.username = pUsername;
    }

    public void setPassword(String pPassword) {
        this.password = pPassword;
    }

    public void setEmail(String pEmail) {
        this.email = pEmail;
    }

    public void setRole(RoleEnum pRoleEnum) {
        this.role = pRoleEnum;
    }


}
