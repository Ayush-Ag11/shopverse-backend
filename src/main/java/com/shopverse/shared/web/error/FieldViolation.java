package com.shopverse.shared.web.error;

public record FieldViolation(
    String field,
    String message
) {
}
