package com.schwab.urlshortener.exception;

import com.schwab.urlshortener.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleInvalidUrlException_returnsBadRequestResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/urls");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidUrlException(
                new InvalidUrlException("Original URL must be a valid HTTP or HTTPS URL"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Original URL must be a valid HTTP or HTTPS URL");
        assertThat(response.getBody().getPath()).isEqualTo("/api/urls");
    }

    @Test
    void handleInactiveUrlException_returnsGoneResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/urls/abc123");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInactiveUrlException(
                new InactiveUrlException("URL is inactive"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Gone");
        assertThat(response.getBody().getMessage()).isEqualTo("URL is inactive");
    }
}
