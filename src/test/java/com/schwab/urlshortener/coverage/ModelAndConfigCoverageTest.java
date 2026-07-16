package com.schwab.urlshortener.coverage;

import com.schwab.urlshortener.config.LoggingConfig;
import com.schwab.urlshortener.config.OpenApiConfig;
import com.schwab.urlshortener.dto.BaseResponse;
import com.schwab.urlshortener.dto.CreateShortUrlRequest;
import com.schwab.urlshortener.dto.CreateShortUrlResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.DashboardSummaryResponse;
import com.schwab.urlshortener.dto.ErrorResponse;
import com.schwab.urlshortener.dto.UrlAnalyticsResponse;
import com.schwab.urlshortener.dto.UrlResponse;
import com.schwab.urlshortener.entity.UrlEntity;
import com.schwab.urlshortener.entity.UrlLinkEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAndConfigCoverageTest {

    @Test
    void dtoBuildersAndGettersExposeExpectedValues() {
        CreateUrlRequest createRequest = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("example")
                .expiryDays(7)
                .build();
        CreateShortUrlRequest shortRequest = CreateShortUrlRequest.builder()
                .targetUrl("https://target.example")
                .build();
        CreateShortUrlResponse shortResponse = CreateShortUrlResponse.builder()
                .alias("abc123")
                .shortUrl("https://short.ly/abc123")
                .targetUrl("https://target.example")
                .build();
        UrlAnalyticsResponse analyticsResponse = UrlAnalyticsResponse.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .clickCount(4)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiryDate(Instant.parse("2024-02-01T00:00:00Z"))
                .lastAccessedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .active(true)
                .build();
        UrlResponse urlResponse = UrlResponse.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("abc123")
                .customAlias("example")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiryDate(Instant.parse("2024-02-01T00:00:00Z"))
                .clickCount(4)
                .active(true)
                .build();
        DashboardSummaryResponse dashboard = DashboardSummaryResponse.builder()
                .totalUrls(10)
                .activeUrls(8)
                .expiredUrls(2)
                .totalClicks(42)
                .topShortCode("abc123")
                .averageClicksPerUrl(4.2)
                .activePercentage(80.0)
                .expiredPercentage(20.0)
                .build();

        assertThat(createRequest.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(createRequest.getCustomAlias()).isEqualTo("example");
        assertThat(createRequest.getExpiryDays()).isEqualTo(7);
        assertThat(shortRequest.getTargetUrl()).isEqualTo("https://target.example");
        assertThat(shortResponse.getAlias()).isEqualTo("abc123");
        assertThat(shortResponse.getShortUrl()).isEqualTo("https://short.ly/abc123");
        assertThat(analyticsResponse.getShortCode()).isEqualTo("abc123");
        assertThat(analyticsResponse.getClickCount()).isEqualTo(4);
        assertThat(urlResponse.getShortCode()).isEqualTo("abc123");
        assertThat(urlResponse.getActive()).isTrue();
        assertThat(dashboard.getTopShortCode()).isEqualTo("abc123");
    }

    @Test
    void baseAndErrorResponsesSupportSuccessAndFailureBuilders() {
        BaseResponse<String> success = BaseResponse.success("created", "ok");
        BaseResponse<String> failure = BaseResponse.failure("failed");
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .status(400)
                .error("Bad Request")
                .message("invalid")
                .path("/api/urls")
                .correlationId("corr-123")
                .build();

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getMessage()).isEqualTo("created");
        assertThat(success.getData()).isEqualTo("ok");
        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getMessage()).isEqualTo("failed");
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getCorrelationId()).isEqualTo("corr-123");
    }

    @Test
    void entityBuildersExposeMutationsAndConfigBeansAreConfigured() {
        UrlEntity entity = UrlEntity.builder()
                .id(10L)
                .originalUrl("https://example.com")
                .shortCode("abc123")
                .customAlias("example")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiryDate(Instant.parse("2024-02-01T00:00:00Z"))
                .clickCount(3)
                .lastAccessedAt(Instant.parse("2024-01-15T00:00:00Z"))
                .active(true)
                .build();
        UrlLinkEntity linkEntity = UrlLinkEntity.builder()
                .id(11L)
                .alias("alias")
                .targetUrl("https://target.example")
                .active(true)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2024-02-01T00:00:00Z"))
                .lastAccessedAt(Instant.parse("2024-01-15T00:00:00Z"))
                .accessCount(5)
                .build();
        OpenApiConfig openApiConfig = new OpenApiConfig();
        LoggingConfig loggingConfig = new LoggingConfig();
        CommonsRequestLoggingFilter filter = loggingConfig.requestLoggingFilter();

        assertThat(entity.getShortCode()).isEqualTo("abc123");
        assertThat(entity.getActive()).isTrue();
        assertThat(linkEntity.getAlias()).isEqualTo("alias");
        assertThat(linkEntity.getAccessCount()).isEqualTo(5);
        assertThat(openApiConfig.customOpenAPI().getInfo().getTitle()).isEqualTo("URL Shortener API");
        assertThat(filter.getClass()).isEqualTo(CommonsRequestLoggingFilter.class);
        assertThat(filter.toString()).contains("CommonsRequestLoggingFilter");
    }
}
