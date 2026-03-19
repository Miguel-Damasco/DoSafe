package com.miguel_damasco.DoSafe.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// @EnableAsync activates Spring's @Async support.
// Without this annotation, @Async methods execute synchronously as regular method calls.
@EnableAsync
@Configuration
public class AsyncConfig {

    // Maximum number of messages SQS delivers per poll (matches maxNumberOfMessages in the consumer).
    // The pool size matches this value so every message in a batch can be processed simultaneously.
    private static final int TEXTRACT_WORKER_COUNT = 5;

    // Maximum concurrent email sends. Large enough to handle bulk alert batches
    // without overwhelming AWS SES rate limits.
    private static final int EMAIL_WORKER_COUNT = 10;

    // Maximum tasks allowed to wait in the queue before CallerRunsPolicy kicks in.
    // If the queue fills up, the scheduler thread executes the task itself,
    // creating natural backpressure instead of dropping tasks.
    private static final int EMAIL_QUEUE_CAPACITY = 500;

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

    // ThreadPoolTaskExecutor is Spring's task executor — required for @Async.
    // The bean name "emailExecutor" matches the value in @Async("emailExecutor"),
    // telling Spring which pool to use for all async email sends across any module.
    @Bean
    public ThreadPoolTaskExecutor emailExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(EMAIL_WORKER_COUNT);
        executor.setMaxPoolSize(EMAIL_WORKER_COUNT);
        executor.setThreadNamePrefix("email-worker-");

        // Bounded queue — prevents unbounded memory growth if emails arrive faster than workers process them.
        executor.setQueueCapacity(EMAIL_QUEUE_CAPACITY);

        // CallerRunsPolicy — when the queue is full, the calling thread (scheduler) executes
        // the task itself instead of rejecting it. This creates natural backpressure:
        // the scheduler slows down automatically without losing any tasks.
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
