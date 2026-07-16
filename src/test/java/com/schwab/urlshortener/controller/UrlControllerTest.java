package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.UrlResponse;
import com.schwab.urlshortener.exception.GlobalExceptionHandler;
import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
@Import(GlobalExceptionHandler.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Test
    void createUrl_whenRequestIsValid_returnsCreated() throws Exception {
        UrlResponse response = UrlResponse.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(true)
                .clickCount(0)
                .build();

        when(urlService.createUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"));
    }

    @Test
    void createUrl_whenRequestIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("valid HTTP or HTTPS URL")));
    }

    @Test
    void redirect_whenServiceReturnsTargetUrl_returnsFound() throws Exception {
        when(urlService.redirect("abc123")).thenReturn("https://example.com");

        mockMvc.perform(get("/api/urls/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void getAnalytics_whenShortCodeMissing_returnsNotFound() throws Exception {
        when(urlService.getAnalytics("missing")).thenThrow(new ShortCodeNotFoundException("Short code not found"));

        mockMvc.perform(get("/api/urls/missing/analytics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short code not found"));
    }
}
