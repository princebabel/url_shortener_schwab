package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.dto.BaseResponse;
import com.schwab.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    /**
     * Returns the current application health status.
     */
    @Tag(name = "Health", description = "Health and availability endpoints")
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return urlShortenerService.health();
    }
}
