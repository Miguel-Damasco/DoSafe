package com.miguel_damasco.DoSafe.alert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}