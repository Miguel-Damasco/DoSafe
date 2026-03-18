package com.miguel_damasco.DoSafe.document.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentModel, UUID> {

    // Returns documents that:
    // - have a known expiration date
    // - expire on or before the given threshold (e.g. today + 30 days)
    // - are fully processed (OCR completed)
    // - do NOT already have an alert — avoids creating duplicate alerts on each daily run
    @Query("""
        SELECT d FROM DocumentModel d
        WHERE d.expireAt IS NOT NULL
          AND d.expireAt <= :threshold
          AND d.status = :status
          AND NOT EXISTS (
              SELECT a FROM AlertModel a WHERE a.document = d
          )
    """)
    List<DocumentModel> findExpiringWithoutAlert(
        @Param("threshold") LocalDate threshold,
        @Param("status") DocumentStatus status
    );
}
