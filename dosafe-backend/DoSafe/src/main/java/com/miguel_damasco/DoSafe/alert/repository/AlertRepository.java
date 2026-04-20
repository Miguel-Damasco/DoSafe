package com.miguel_damasco.DoSafe.alert.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;

@Repository
public interface AlertRepository extends JpaRepository<AlertModel, String> {

    // Returns all unsent alerts with their user and document eagerly loaded.
    // JOIN FETCH is required because user and document are LAZY — without it,
    // accessing them in the @Async thread throws LazyInitializationException
    // since the JPA session is already closed by then.
    @Query("SELECT a FROM AlertModel a JOIN FETCH a.user JOIN FETCH a.document WHERE a.sentAt IS NULL")
    List<AlertModel> findAllBySentAtIsNull();

    // Counts alerts that were sent to the user but not yet read (unread = bell badge count).
    long countByUserUsernameAndSentAtIsNotNullAndReadAtIsNull(String pUsername);

    // Returns sent alerts for a user ordered newest-first (for the notification panel).
    // JOIN FETCH user and document so the mapper can access them without lazy load issues.
    @Query("SELECT a FROM AlertModel a JOIN FETCH a.user JOIN FETCH a.document " +
           "WHERE a.user.username = :username AND a.sentAt IS NOT NULL " +
           "ORDER BY a.createdAt DESC")
    Page<AlertModel> findSentAlertsByUsername(@Param("username") String pUsername, Pageable pPageable);

    // Bulk-marks all unread sent alerts as read for a given user.
    @Modifying
    @Query("UPDATE AlertModel a SET a.readAt = :now " +
           "WHERE a.user.username = :username AND a.sentAt IS NOT NULL AND a.readAt IS NULL")
    int markAllReadByUsername(@Param("username") String pUsername, @Param("now") Instant pNow);

    // Deletes all alerts linked to a document — called before the document itself is deleted
    // to avoid FK constraint violations.
    void deleteByDocumentId(UUID pDocumentId);
}