package com.miguel_damasco.DoSafe.email.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.email.infrastructure.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    // Depends on the abstraction, not on any specific provider.
    private final EmailSender emailSender;

    // @Async("emailExecutor") — Spring runs this method in a thread from the emailExecutor pool.
    // The calling thread returns immediately with a CompletableFuture.
    //
    // Returning CompletableFuture<Void> lets callers chain post-send logic via .thenRun()
    // and handle failures via .exceptionally() — without coupling that logic to this class.
    // Example: AlertService chains markSent() + alertRepository.save() after the send succeeds.
    @Async("emailExecutor")
    public CompletableFuture<Void> send(String pTo, String pSubject, String pBody) {
        emailSender.send(pTo, pSubject, pBody);
        return CompletableFuture.completedFuture(null);
    }
}
