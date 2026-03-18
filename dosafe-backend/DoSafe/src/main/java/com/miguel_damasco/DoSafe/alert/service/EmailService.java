package com.miguel_damasco.DoSafe.alert.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;
import com.miguel_damasco.DoSafe.alert.infrastructure.EmailSender;
import com.miguel_damasco.DoSafe.alert.repository.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    // Depends on the abstraction, not the AWS implementation.
    // To switch providers (SendGrid, Mailgun, etc.), create a new EmailSender
    // implementation and swap the @Component — EmailService needs zero changes.
    private final EmailSender emailSender;
    private final AlertRepository alertRepository;

    // @Async("alertExecutor") — Spring executes this method in a thread from the alertExecutor pool.
    // The calling thread (scheduling-1) returns immediately without waiting for the email to be sent.
    // This allows AlertService to dispatch all pending alerts concurrently instead of one by one.
    //
    // markSent() and save() live here — not in AlertService — because they must happen
    // AFTER the email is confirmed sent, inside the same async thread.
    // If they stayed in AlertService, they would execute before the email was actually sent.
    @Async("alertExecutor")
    public void sendExpirationAlert(AlertModel pAlert) {

        try {
            log.info("Sending expiration alert userId={} documentId={}",
                    pAlert.getUser().getId(),
                    pAlert.getDocument().getId());

            emailSender.send(pAlert);
            pAlert.markSent();
            alertRepository.save(pAlert);

            log.info("Alert sent and marked alertId={}", pAlert.getId());

        } catch (Exception e) {
            // Log the failure but do not rethrow — the alert stays with sentAt = null
            // and will be retried on the next daily run.
            log.error("Failed to send alert alertId={} — will retry on next run",
                    pAlert.getId(), e);
        }
    }
}
