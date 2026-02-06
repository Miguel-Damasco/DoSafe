package com.miguel_damasco.DoSafe.document.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentModel, UUID> {}
