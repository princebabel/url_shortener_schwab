package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.BaseResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlShortenerServiceImplTest {

    private final UrlShortenerServiceImpl service = new UrlShortenerServiceImpl();

    @Test
    void health_returnsSuccessfulBaseResponse() {
        BaseResponse<String> response = service.health();

        assertTrue(response.isSuccess());
        assertEquals("Service is up and running", response.getMessage());
        assertEquals("UP", response.getData());
        assertTrue(response.getTimestamp() != null);
    }
}
