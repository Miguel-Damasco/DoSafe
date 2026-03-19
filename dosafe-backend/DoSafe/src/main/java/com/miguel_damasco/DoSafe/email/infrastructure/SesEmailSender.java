package com.miguel_damasco.DoSafe.email.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
// AWS SES implementation of EmailSender.
// To switch to another provider (SendGrid, Mailgun, etc.), create a new @Component
// implementing EmailSender and remove @Component from this class — callers need zero changes.
public class SesEmailSender implements EmailSender {

    private final SesClient sesClient;

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    @Override
    public void send(String pTo, String pSubject, String pBody) {

        SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail.strip())
                .destination(Destination.builder()
                        .toAddresses(pTo.strip())
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data(pSubject)
                                .charset("UTF-8")
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .data(pBody)
                                        .charset("UTF-8")
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);

        log.info("Email sent to={}", pTo.strip());
    }
}
