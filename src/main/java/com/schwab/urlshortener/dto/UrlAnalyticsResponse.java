package com.schwab.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAnalyticsResponse {

    private String shortCode;
    private String originalUrl;
    private Integer clickCount;
    private Instant createdAt;
    private Instant expiryDate;
    private Instant lastAccessedAt;
    private Boolean active;
}
