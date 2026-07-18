package com.shopverse.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final int MAX_CORRELATION_ID_LENGTH = 64;

    private static final Pattern VALID_CORRELATION_ID =
        Pattern.compile("[A-Za-z0-9._-]{1," + MAX_CORRELATION_ID_LENGTH + "}");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(
            request.getHeader(CorrelationId.HEADER_NAME)
        );

        String previousCorrelationId = MDC.get(CorrelationId.MDC_KEY);

        request.setAttribute(
            CorrelationId.REQUEST_ATTRIBUTE,
            correlationId
        );

        response.setHeader(
            CorrelationId.HEADER_NAME,
            correlationId
        );

        MDC.put(CorrelationId.MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            restorePreviousMdcValue(previousCorrelationId);
        }
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null
            || incomingCorrelationId.isBlank()
            || !VALID_CORRELATION_ID.matcher(incomingCorrelationId).matches()) {

            return UUID.randomUUID().toString();
        }

        return incomingCorrelationId;
    }

    private void restorePreviousMdcValue(String previousCorrelationId) {
        if (previousCorrelationId == null) {
            MDC.remove(CorrelationId.MDC_KEY);
            return;
        }

        MDC.put(CorrelationId.MDC_KEY, previousCorrelationId);
    }
}
