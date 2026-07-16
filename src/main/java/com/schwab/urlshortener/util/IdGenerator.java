package com.schwab.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class IdGenerator {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateAlias() {
        return generateAlias(DEFAULT_LENGTH);
    }

    public String generateAlias(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
