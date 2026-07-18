package com.shopverse.shared.web.error;

import com.shopverse.shared.web.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE =
        "An unexpected error occurred.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        var fieldViolations = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldViolation(
                error.getField(),
                resolveValidationMessage(error.getDefaultMessage())
            ));

        var globalViolations = exception
            .getBindingResult()
            .getGlobalErrors()
            .stream()
            .map(error -> new FieldViolation(
                "$request",
                resolveValidationMessage(error.getDefaultMessage())
            ));

        var violations = Stream
            .concat(fieldViolations, globalViolations)
            .sorted(
                Comparator.comparing(FieldViolation::field)
                    .thenComparing(FieldViolation::message)
            )
            .toList();

        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(
                status,
                "One or more request fields are invalid."
            );

        problemDetail.setTitle("Validation failed");
        problemDetail.setProperty("violations", violations);

        return handleExceptionInternal(
            exception,
            problemDetail,
            headers,
            status,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(
                status,
                "The request body is missing, malformed, or contains invalid data."
            );

        problemDetail.setTitle("Malformed request");

        return handleExceptionInternal(
            exception,
            problemDetail,
            headers,
            status,
            request
        );
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnexpectedException(
        Exception exception,
        WebRequest request
    ) {
        log.error(
            "Unhandled exception while processing request",
            exception
        );

        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_INTERNAL_ERROR_MESSAGE
            );

        problemDetail.setTitle("Internal server error");

        return handleExceptionInternal(
            exception,
            problemDetail,
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        ResponseEntity<Object> response = super.handleExceptionInternal(
            exception,
            body,
            headers,
            statusCode,
            request
        );

        if (response == null) {
            return null;
        }

        if (response.getBody() instanceof ProblemDetail problemDetail) {
            decorateProblemDetail(
                problemDetail,
                exception,
                statusCode,
                request
            );
        }

        return response;
    }

    private void decorateProblemDetail(
        ProblemDetail problemDetail,
        Exception exception,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        problemDetail.setProperty(
            "code",
            resolveErrorCode(exception, statusCode).name()
        );

        problemDetail.setProperty(
            "timestamp",
            Instant.now()
        );

        problemDetail.setProperty(
            "correlationId",
            resolveCorrelationId(request)
        );

        if (problemDetail.getInstance() == null) {
            resolveRequestPath(request)
                .map(URI::create)
                .ifPresent(problemDetail::setInstance);
        }
    }

    private ApiErrorCode resolveErrorCode(
        Exception exception,
        HttpStatusCode statusCode
    ) {
        if (exception instanceof MethodArgumentNotValidException) {
            return ApiErrorCode.VALIDATION_FAILED;
        }

        if (exception instanceof HttpMessageNotReadableException) {
            return ApiErrorCode.MALFORMED_REQUEST;
        }

        if (exception instanceof ErrorResponse errorResponse) {
            int errorStatus = errorResponse.getStatusCode().value();

            return switch (errorStatus) {
                case 404 -> ApiErrorCode.RESOURCE_NOT_FOUND;
                case 405 -> ApiErrorCode.METHOD_NOT_ALLOWED;
                case 415 -> ApiErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> resolveCodeFromStatus(errorStatus);
            };
        }

        return resolveCodeFromStatus(statusCode.value());
    }

    private ApiErrorCode resolveCodeFromStatus(int statusCode) {
        if (statusCode >= 500) {
            return ApiErrorCode.INTERNAL_SERVER_ERROR;
        }

        if (statusCode == 404) {
            return ApiErrorCode.RESOURCE_NOT_FOUND;
        }

        return ApiErrorCode.REQUEST_REJECTED;
    }

    private String resolveValidationMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Invalid value";
        }

        return message;
    }

    private String resolveCorrelationId(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            Object attribute = servletWebRequest
                .getRequest()
                .getAttribute(CorrelationId.REQUEST_ATTRIBUTE);

            if (attribute instanceof String correlationId
                && !correlationId.isBlank()) {
                return correlationId;
            }
        }

        String mdcCorrelationId = MDC.get(CorrelationId.MDC_KEY);

        if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
            return mdcCorrelationId;
        }

        return "unavailable";
    }

    private java.util.Optional<String> resolveRequestPath(
        WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return java.util.Optional.of(
                servletWebRequest
                    .getRequest()
                    .getRequestURI()
            );
        }

        return java.util.Optional.empty();
    }
}
