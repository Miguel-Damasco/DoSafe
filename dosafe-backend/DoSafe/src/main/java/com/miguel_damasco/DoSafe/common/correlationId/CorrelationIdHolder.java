package com.miguel_damasco.DoSafe.common.correlationId;

import org.slf4j.MDC;

public class CorrelationIdHolder {
    
    private static final String CORRELATION_ID = "correlationId";

    public static String get() {
        return MDC.get(CORRELATION_ID);
    }

    public static void set(String pColletaionId) {
        MDC.put(CORRELATION_ID, pColletaionId);
    }

    public static void clear() {
        MDC.clear();
    }
}
