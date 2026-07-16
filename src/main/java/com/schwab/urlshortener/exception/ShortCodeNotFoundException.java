package com.schwab.urlshortener.exception;

public class ShortCodeNotFoundException extends RuntimeException {

    public ShortCodeNotFoundException(String message) {
        super(message);
    }
}
