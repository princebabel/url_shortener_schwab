package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.DashboardSummaryResponse;
import com.schwab.urlshortener.dto.UrlAnalyticsResponse;
import com.schwab.urlshortener.dto.UrlResponse;
import com.schwab.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    /**
     * Creates a new short URL for a valid long URL.
     */
    @Tag(name = "URLs", description = "Create, manage, and resolve short URLs")
    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = urlService.createUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Redirects a short code to its original destination.
     */
    @Tag(name = "URLs", description = "Create, manage, and resolve short URLs")
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String targetUrl = urlService.redirect(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(targetUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Returns analytics for a specific short URL.
     */
    @Tag(name = "Analytics", description = "Retrieve analytics and dashboard summaries")
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getAnalytics(shortCode));
    }

    /**
     * Lists URLs with optional search, filtering, and pagination controls.
     */
    @Tag(name = "URLs", description = "Create, manage, and resolve short URLs")
    @GetMapping
    public ResponseEntity<Page<UrlResponse>> getUrls(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(urlService.getUrls(search, active, expired, pageable));
    }

    /**
     * Searches URLs by short code, original URL, or custom alias.
     */
    @Tag(name = "URLs", description = "Create, manage, and resolve short URLs")
    @GetMapping("/search")
    public ResponseEntity<Page<UrlResponse>> searchUrls(@RequestParam String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(urlService.getUrls(query, null, null, pageable));
    }

    /**
     * Returns a high-level dashboard summary for the service.
     */
    @Tag(name = "Analytics", description = "Retrieve analytics and dashboard summaries")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(urlService.getDashboardSummary());
    }

    /**
     * Returns the highest-click URLs for dashboard display.
     */
    @Tag(name = "Analytics", description = "Retrieve analytics and dashboard summaries")
    @GetMapping("/dashboard/top")
    public ResponseEntity<Page<UrlResponse>> getTopUrls(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(urlService.getTopUrls(pageable));
    }

    /**
     * Returns the most recently created URLs for dashboard display.
     */
    @Tag(name = "Analytics", description = "Retrieve analytics and dashboard summaries")
    @GetMapping("/dashboard/recent")
    public ResponseEntity<Page<UrlResponse>> getRecentUrls(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(urlService.getRecentUrls(pageable));
    }
}
