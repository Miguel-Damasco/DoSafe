package com.miguel_damasco.DoSafe.common.correlationId;

import org.slf4j.MDC;

public class CorrelationIdHolder {

    private static final String CORRELATION_ID = "correlationId";

    // Returns the correlationId stored in the current thread's MDC.
    // Returns null if no correlationId has been set for this thread.
    public static String get() {
        return MDC.get(CORRELATION_ID);
    }

    // Stores the correlationId in the current thread's MDC.
    // The MDC is thread-local, so this only affects the calling thread.
    public static void set(String pCorrelationId) {
        MDC.put(CORRELATION_ID, pCorrelationId);
    }

    // Removes only the correlationId key from the MDC.
    // Uses remove() instead of MDC.clear() to avoid wiping other entries
    // that Spring Security or other components may have added to the MDC.
    public static void clear() {
        MDC.remove(CORRELATION_ID);
    }

    // Wraps a task so that the correlationId of the current thread is propagated
    // to the thread that will execute the task.
    //
    // The problem: MDC is thread-local. When a new thread is spawned (e.g. via
    // CompletableFuture), it starts with an empty MDC — the correlationId from
    // the parent thread is lost.
    //
    // The solution: capture the correlationId before the task is submitted,
    // then restore it inside the new thread at execution time.
    //
    // Usage:
    //   CompletableFuture.runAsync(CorrelationIdHolder.wrap(() -> process(message)), executor);
    public static Runnable wrap(Runnable task) {

        // Captured here, in the parent thread, before the task is submitted.
        String correlationId = get();

        return () -> {
            // We are now in the child thread. Restore the correlationId into its MDC.
            if (correlationId != null) {
                set(correlationId);
            }
            try {
                task.run();
            } finally {
                // Always clean up the MDC at the end of the task,
                // regardless of whether it succeeded or threw an exception.
                // This prevents correlationId leaks when threads are reused from a pool.
                clear();
            }
        };
    }
}
