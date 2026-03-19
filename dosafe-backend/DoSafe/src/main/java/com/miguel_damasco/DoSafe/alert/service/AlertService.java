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
import com.miguel_damasco.DoSafe.email.service.EmailService;

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

    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiringDocuments() {

        log.info("Starting daily expiration alert check thresholdDays={}", expirationThresholdDays);

        LocalDate threshold = LocalDate.now().plusDays(expirationThresholdDays);

        // Step 1 — find documents expiring within the threshold that don't have an alert yet.
        List<DocumentModel> expiringDocuments = documentRepository.findExpiringWithoutAlert(
                threshold,
                DocumentStatus.PROCESSED
        );

        if (!expiringDocuments.isEmpty()) {
            log.info("Found expiring documents count={}", expiringDocuments.size());

            // Step 2 — create an alert record for each document found.
            List<AlertModel> newAlerts = expiringDocuments.stream()
                    .map(this::buildAlert)
                    .toList();

            alertRepository.saveAll(newAlerts);

            log.info("Alerts created count={}", newAlerts.size());
        } else {
            log.info("No new expiring documents found");
        }

        // Step 3 — always send pending alerts, even if no new documents were found.
        // This retries any alerts that failed to send in a previous run (sentAt = null).
        sendPendingAlerts();
    }

    // Dispatches emails for all alerts with sentAt = null.
    // EmailService.send() is @Async — each call returns a CompletableFuture immediately.
    // Post-send logic (markSent + save) is chained via thenRun() so it runs inside the
    // email-worker thread AFTER the send succeeds, not before.
    private void sendPendingAlerts() {

        List<AlertModel> pendingAlerts = alertRepository.findAllBySentAtIsNull();

        log.info("Dispatching pending alerts count={}", pendingAlerts.size());

        pendingAlerts.forEach(alert -> {
            String to = alert.getUser().getEmail();
            String subject = buildSubject(alert);
            String body = buildBody(alert);

            emailService.send(to, subject, body)
                    .thenRun(() -> {
                        alert.markSent();
                        alertRepository.save(alert);
                        log.info("Alert sent and marked alertId={}", alert.getId());
                    })
                    .exceptionally(ex -> {
                        // Log the failure but do not rethrow — the alert stays with sentAt = null
                        // and will be retried on the next daily run.
                        log.error("Failed to send alert alertId={} — will retry on next run",
                                alert.getId(), ex);
                        return null;
                    });
        });
    }

    private AlertModel buildAlert(DocumentModel pDocument) {
        AlertModel alert = new AlertModel();
        alert.setUser(pDocument.getUser());
        alert.setDocument(pDocument);
        alert.setCreatedAt(Instant.now());
        return alert;
    }

    private String buildSubject(AlertModel pAlert) {
        return String.format("Your %s expires on %s",
                pAlert.getDocument().getType(),
                pAlert.getDocument().getExpireAt());
    }

    private String buildBody(AlertModel pAlert) {
        return String.format("""
                Hi %s,

                This is a reminder that your %s expires on %s.

                Please renew it before the expiration date to avoid any issues.

                DoSafe Team
                """,
                pAlert.getUser().getUsername(),
                pAlert.getDocument().getType(),
                pAlert.getDocument().getExpireAt());
    }
}
