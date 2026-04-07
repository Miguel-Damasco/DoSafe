package com.miguel_damasco.DoSafe.document.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.user.domain.UserModel;

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

    // Returns a paginated slice of documents belonging to a user, ordered by the
    // Pageable sort (typically createdAt DESC so newest documents appear first).
    Page<DocumentModel> findByUser(UserModel user, Pageable pageable);

    // Returns a paginated slice of documents belonging to a user filtered by type.
    // Spring Data derives the query from the method name — no @Query needed.
    Page<DocumentModel> findByUserAndType(UserModel user, DocumentTypeEnum type, Pageable pageable);

    // Returns expired documents for a user: status PROCESSED, expireAt known and
    // strictly before today. Only PROCESSED documents have a real expiration date
    // extracted by OCR, so filtering by status avoids returning stale PROCESSING rows.
    // Sorting is delegated to the Pageable (typically expireAt ASC).
    @Query("""
        SELECT d FROM DocumentModel d
        WHERE d.user = :user
          AND d.status = :status
          AND d.expireAt IS NOT NULL
          AND d.expireAt < :today
    """)
    Page<DocumentModel> findExpiredByUser(
        @Param("user") UserModel user,
        @Param("status") DocumentStatus status,
        @Param("today") LocalDate today,
        Pageable pageable
    );

    // Counts how many documents a specific user uploaded within a time window.
    // Used to enforce the per-user daily upload limit.
    long countByUserAndCreatedAtBetween(UserModel user, Instant start, Instant end);

    // Counts all documents uploaded across all users within a time window.
    // Used to enforce the global daily upload limit.
    long countByCreatedAtBetween(Instant start, Instant end);
}
