package com.schwab.urlshortener.exception;

public class InactiveUrlException extends RuntimeException {

    public InactiveUrlException(String message) {
        super(message);
    }
}
