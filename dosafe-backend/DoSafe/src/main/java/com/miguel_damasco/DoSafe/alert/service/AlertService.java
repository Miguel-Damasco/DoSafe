package com.miguel_damasco.DoSafe.alert.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;
import com.miguel_damasco.DoSafe.alert.repository.AlertRepository;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertService {

    private final DocumentRepository documentRepository;
    private final AlertRepository alertRepository;
    private final EmailService emailService;

    // How many days before expiration to trigger the alert — configurable via application.properties.
    @Value("${alert.expiration-threshold-days:30}")
    private int expirationThresholdDays;

    // TEMPORARY — runs every minute for testing. Change back to "0 0 8 * * *" after testing.
    @Scheduled(cron = "0 * * * * *")
    public void checkExpiringDocuments() {

        log.info("Starting daily expiration alert check thresholdDays={}", expirationThresholdDays);

        LocalDate threshold = LocalDate.now().plusDays(expirationThresholdDays);

        // Step 1 — find documents expiring within the threshold that don't have an alert yet.
        List<DocumentModel> expiringDocuments = documentRepository.findExpiringWithoutAlert(
                threshold,
                DocumentStatus.PROCESSED
        );

        if (expiringDocuments.isEmpty()) {
            log.info("No expiring documents found");
            return;
        }

        log.info("Found expiring documents count={}", expiringDocuments.size());

        // Step 2 — create an alert record for each document found.
        List<AlertModel> newAlerts = expiringDocuments.stream()
                .map(this::buildAlert)
                .toList();

        alertRepository.saveAll(newAlerts);

        log.info("Alerts created count={}", newAlerts.size());

        // Step 3 — send emails for all unsent alerts (includes any that may have failed previously).
        sendPendingAlerts();
    }

    // Dispatches emails for all alerts with sentAt = null.
    // Each send is async — this method returns immediately after firing all tasks.
    // markSent() and error handling live inside EmailService, in the async thread.
    private void sendPendingAlerts() {

        List<AlertModel> pendingAlerts = alertRepository.findAllBySentAtIsNull();

        log.info("Dispatching pending alerts count={}", pendingAlerts.size());

        // Each call returns immediately — Spring fires the email in an alert-worker thread.
        pendingAlerts.forEach(emailService::sendExpirationAlert);
    }

    private AlertModel buildAlert(DocumentModel pDocument) {
        AlertModel alert = new AlertModel();
        alert.setUser(pDocument.getUser());
        alert.setDocument(pDocument);
        alert.setCreatedAt(Instant.now());
        return alert;
    }
}
