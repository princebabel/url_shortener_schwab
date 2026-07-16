package com.schwab.urlshortener.validation;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class UrlValidator {

    public boolean isValid(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(targetUrl);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
