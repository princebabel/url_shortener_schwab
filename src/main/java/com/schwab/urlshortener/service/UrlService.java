package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.DashboardSummaryResponse;
import com.schwab.urlshortener.dto.UrlAnalyticsResponse;
import com.schwab.urlshortener.dto.UrlResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UrlService {

    /**
     * Creates a new short URL from a valid long URL and optional custom alias.
     */
    UrlResponse createUrl(CreateUrlRequest request);

    /**
     * Resolves a short code to its original URL while tracking clicks and enforcing link state.
     */
    String redirect(String shortCode);

    /**
     * Returns the analytics view for a specific short URL.
     */
    UrlAnalyticsResponse getAnalytics(String shortCode);

    /**
     * Lists URLs with optional search, filtering, and pagination support.
     */
    Page<UrlResponse> getUrls(String search, Boolean active, Boolean expired, Pageable pageable);

    /**
     * Returns a dashboard summary of link and click activity.
     */
    DashboardSummaryResponse getDashboardSummary();

    /**
     * Returns the most clicked URLs for dashboard display.
     */
    Page<UrlResponse> getTopUrls(Pageable pageable);

    /**
     * Returns the most recently created URLs for dashboard display.
     */
    Page<UrlResponse> getRecentUrls(Pageable pageable);
}
