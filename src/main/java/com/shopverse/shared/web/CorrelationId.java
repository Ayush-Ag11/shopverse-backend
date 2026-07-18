package com.shopverse.shared.web;

public final class CorrelationId {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String REQUEST_ATTRIBUTE = "shopverse.correlationId";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
        throw new UnsupportedOperationException(
            "CorrelationId is a utility class and cannot be instantiated"
        );
    }
}
