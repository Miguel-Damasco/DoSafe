package com.miguel_damasco.DoSafe.alert.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@RequiredArgsConstructor
@Component
public class SesEmailSender implements EmailSender {

    private final SesClient sesClient;

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    @Override
    public void send(AlertModel pAlert) {

        String recipientEmail = pAlert.getUser().getEmail();
        String subject = buildSubject(pAlert);
        String body = buildBody(pAlert);

        SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder()
                        .toAddresses(recipientEmail)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data(subject)
                                .charset("UTF-8")
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .data(body)
                                        .charset("UTF-8")
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);

        log.info("Expiration alert email sent to={} documentId={}",
                recipientEmail,
                pAlert.getDocument().getId());
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
