package com.shopverse.shared.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter =
        new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldReuseValidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String incomingCorrelationId = "checkout-request_2026.07-15";

        request.addHeader(
            CorrelationId.HEADER_NAME,
            incomingCorrelationId
        );

        AtomicReference<String> correlationIdInsideChain =
            new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) ->
            correlationIdInsideChain.set(
                MDC.get(CorrelationId.MDC_KEY)
            );

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationId.HEADER_NAME))
            .isEqualTo(incomingCorrelationId);

        assertThat(request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE))
            .isEqualTo(incomingCorrelationId);

        assertThat(correlationIdInsideChain.get())
            .isEqualTo(incomingCorrelationId);

        assertThat(MDC.get(CorrelationId.MDC_KEY))
            .isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
            request,
            response,
            (servletRequest, servletResponse) -> {
            }
        );

        String generatedCorrelationId =
            response.getHeader(CorrelationId.HEADER_NAME);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();

        assertThat(request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE))
            .isEqualTo(generatedCorrelationId);

        assertThat(MDC.get(CorrelationId.MDC_KEY))
            .isNull();
    }

    @Test
    void shouldReplaceInvalidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String invalidCorrelationId = "invalid correlation id";

        request.addHeader(
            CorrelationId.HEADER_NAME,
            invalidCorrelationId
        );

        filter.doFilter(
            request,
            response,
            (servletRequest, servletResponse) -> {
            }
        );

        String generatedCorrelationId =
            response.getHeader(CorrelationId.HEADER_NAME);

        assertThat(generatedCorrelationId)
            .isNotEqualTo(invalidCorrelationId);

        assertThat(UUID.fromString(generatedCorrelationId))
            .isNotNull();
    }

    @Test
    void shouldRestoreExistingMdcValueAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MDC.put(CorrelationId.MDC_KEY, "outer-correlation-id");

        AtomicReference<String> correlationIdInsideChain =
            new AtomicReference<>();

        filter.doFilter(
            request,
            response,
            (servletRequest, servletResponse) ->
                correlationIdInsideChain.set(
                    MDC.get(CorrelationId.MDC_KEY)
                )
        );

        assertThat(correlationIdInsideChain.get())
            .isNotEqualTo("outer-correlation-id");

        assertThat(MDC.get(CorrelationId.MDC_KEY))
            .isEqualTo("outer-correlation-id");
    }
}
