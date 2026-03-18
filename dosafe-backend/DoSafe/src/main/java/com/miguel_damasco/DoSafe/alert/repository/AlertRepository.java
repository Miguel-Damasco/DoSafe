package com.miguel_damasco.DoSafe.alert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;

@Repository
public interface AlertRepository extends JpaRepository<AlertModel, String> {

    // Returns all alerts whose email has not been sent yet (sentAt is null).
    // Used by the scheduler to retry sending in case a previous attempt failed.
    List<AlertModel> findAllBySentAtIsNull();
}