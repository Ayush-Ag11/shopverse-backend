package com.shopverse.shared.exception;

public abstract class ShopVerseException extends RuntimeException {

    protected ShopVerseException(String message) {
        super(validateMessage(message));
    }

    private static String validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                "ShopVerse exception message must not be blank"
            );
        }

        return message;
    }
}
