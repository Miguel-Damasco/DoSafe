package com.miguel_damasco.DoSafe.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncConfig {

    // Maximum number of messages SQS delivers per poll (matches maxNumberOfMessages in the consumer).
    // The pool size matches this value so every message in a batch can be processed simultaneously.
    private static final int TEXTRACT_WORKER_COUNT = 5;

    @Bean
    public ExecutorService textractExecutor() {

        AtomicInteger counter = new AtomicInteger(1);

        return Executors.newFixedThreadPool(TEXTRACT_WORKER_COUNT, task -> {

            Thread thread = new Thread(task);

            // Named threads make it easy to identify workers in logs and thread dumps.
            // e.g. "textract-worker-1" instead of "pool-1-thread-1"
            thread.setName("textract-worker-" + counter.getAndIncrement());

            // Daemon threads don't block JVM shutdown if they're still running.
            thread.setDaemon(true);

            return thread;
        });
    }
}
