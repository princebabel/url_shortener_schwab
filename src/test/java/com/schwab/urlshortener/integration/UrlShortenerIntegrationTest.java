package com.schwab.urlshortener.integration;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.UrlAnalyticsResponse;
import com.schwab.urlshortener.dto.UrlResponse;
import com.schwab.urlshortener.entity.UrlEntity;
import com.schwab.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Runs against H2 in-memory database.
 * For full production-fidelity validation against real PostgreSQL, see the
 * Testcontainers-based version (requires Docker) — see
 * docs/others/engineering-decisions.md for the trade-off rationale.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
class UrlShortenerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlRepository urlRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/urls";
        urlRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        urlRepository.deleteAll();
    }

    // =========================================================================
    // FULL CREATE-TO-REDIRECT FLOW
    // =========================================================================

    @Test
    void createAndRedirect_fullFlow_returnsCreatedAndRedirects() {
        // Step 1: Create a short URL
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com/very/long/url/path?query=param")
                .build();

        ResponseEntity<UrlResponse> createResponse = restTemplate.postForEntity(
                baseUrl,
                request,
                UrlResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        UrlResponse created = createResponse.getBody();
        assertThat(created.getShortCode()).isNotBlank();
        assertThat(created.getOriginalUrl()).isEqualTo("https://example.com/very/long/url/path?query=param");
        assertThat(created.getActive()).isTrue();
        assertThat(created.getClickCount()).isZero();

        String shortCode = created.getShortCode();

        // Step 2: Redirect using the short code
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html");
        HttpEntity<Void> redirectRequest = new HttpEntity<>(headers);

        ResponseEntity<Void> redirectResponse = restTemplate.exchange(
                baseUrl + "/" + shortCode,
                HttpMethod.GET,
                redirectRequest,
                Void.class
        );

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectResponse.getHeaders().getLocation()).isNotNull();
        assertThat(redirectResponse.getHeaders().getLocation().toString())
                .isEqualTo("https://example.com/very/long/url/path?query=param");

        // Step 3: Verify click count incremented
        ResponseEntity<UrlAnalyticsResponse> analyticsResponse = restTemplate.getForEntity(
                baseUrl + "/" + shortCode + "/analytics",
                UrlAnalyticsResponse.class
        );

        assertThat(analyticsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(analyticsResponse.getBody()).isNotNull();
        assertThat(analyticsResponse.getBody().getClickCount()).isEqualTo(1);
    }

    @Test
    void createWithCustomAlias_returnsCustomShortCode() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("my-custom-alias")
                .build();

        ResponseEntity<UrlResponse> response = restTemplate.postForEntity(
                baseUrl,
                request,
                UrlResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getShortCode()).isEqualTo("my-custom-alias");
    }

    @Test
    void createWithExpiryDays_setsExpiryOnEntity() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .expiryDays(1)
                .build();

        ResponseEntity<UrlResponse> response = restTemplate.postForEntity(
                baseUrl,
                request,
                UrlResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getExpiryDate()).isNotNull();
    }

    // =========================================================================
    // EDGE CASE: INVALID URL (400 Bad Request)
    // =========================================================================

    @Test
    void createWithInvalidUrl_returnsBadRequest() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("not-a-valid-url")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("valid HTTP or HTTPS URL");
    }

    @Test
    void createWithEmptyUrl_returnsBadRequest() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createWithFtpUrl_returnsBadRequest() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("ftp://example.com/file.txt")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createWithNullUrl_returnsBadRequest() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl(null)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // EDGE CASE: ALIAS COLLISION (409 Conflict)
    // =========================================================================

    @Test
    void createWithDuplicateCustomAlias_returnsConflict() {
        CreateUrlRequest request1 = CreateUrlRequest.builder()
                .originalUrl("https://example.com/first")
                .customAlias("duplicate-alias")
                .build();

        CreateUrlRequest request2 = CreateUrlRequest.builder()
                .originalUrl("https://example.com/second")
                .customAlias("duplicate-alias")
                .build();

        // First creation succeeds
        ResponseEntity<UrlResponse> response1 = restTemplate.postForEntity(
                baseUrl,
                request1,
                UrlResponse.class
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second creation with same alias fails
        ResponseEntity<String> response2 = restTemplate.postForEntity(
                baseUrl,
                request2,
                String.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response2.getBody()).contains("already exists");
    }

    @Test
    void createWithAliasMatchingExistingShortCode_returnsConflict() {
        // First create a URL (auto-generated short code)
        CreateUrlRequest request1 = CreateUrlRequest.builder()
                .originalUrl("https://example.com/first")
                .build();

        ResponseEntity<UrlResponse> response1 = restTemplate.postForEntity(
                baseUrl,
                request1,
                UrlResponse.class
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String generatedShortCode = response1.getBody().getShortCode();

        // Try to create another URL with custom alias matching the generated short code
        CreateUrlRequest request2 = CreateUrlRequest.builder()
                .originalUrl("https://example.com/second")
                .customAlias(generatedShortCode)
                .build();

        ResponseEntity<String> response2 = restTemplate.postForEntity(
                baseUrl,
                request2,
                String.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // =========================================================================
    // EDGE CASE: EXPIRED / MISSING SHORT CODE (410 Gone / 404 Not Found)
    // =========================================================================

    @Test
    void redirectToExpiredUrl_returnsGone() {
        // Create an expired URL directly in the database
        UrlEntity expiredEntity = UrlEntity.builder()
                .shortCode("expired123")
                .originalUrl("https://example.com/expired")
                .active(true)
                .expiryDate(Instant.now().minusSeconds(60)) // Expired 1 minute ago
                .clickCount(0)
                .createdAt(Instant.now())
                .build();
        urlRepository.saveAndFlush(expiredEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/expired123",
                HttpMethod.GET,
                request,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void redirectToInactiveUrl_returnsGone() {
        // Create an inactive URL directly in the database
        UrlEntity inactiveEntity = UrlEntity.builder()
                .shortCode("inactive123")
                .originalUrl("https://example.com/inactive")
                .active(false)
                .expiryDate(Instant.now().plusSeconds(3600))
                .clickCount(0)
                .createdAt(Instant.now())
                .build();
        urlRepository.saveAndFlush(inactiveEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/inactive123",
                HttpMethod.GET,
                request,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void redirectToNonExistentShortCode_returnsNotFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/nonexistent123",
                HttpMethod.GET,
                request,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void analyticsForNonExistentShortCode_returnsNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/nonexistent123/analytics",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("not found");
    }

    @Test
    void analyticsForExpiredUrl_returnsGone() {
        UrlEntity expiredEntity = UrlEntity.builder()
                .shortCode("expired-analytics")
                .originalUrl("https://example.com/expired")
                .active(true)
                .expiryDate(Instant.now().minusSeconds(60))
                .clickCount(5)
                .createdAt(Instant.now())
                .build();
        urlRepository.saveAndFlush(expiredEntity);

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/expired-analytics/analytics",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    // =========================================================================
    // EDGE CASE: CONCURRENT CREATION
    // =========================================================================

    @Test
    void concurrentCreationWithSameCustomAlias_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        String customAlias = "concurrent-alias";
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    CreateUrlRequest request = CreateUrlRequest.builder()
                            .originalUrl("https://example.com/thread-" + threadNum)
                            .customAlias(customAlias)
                            .build();

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            baseUrl,
                            request,
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                        conflictCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Exactly one should succeed, rest should get conflict
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
        assertThat(errorCount.get()).isEqualTo(0);
    }

    @Test
    void concurrentCreationWithDifferentUrls_allSucceedWithUniqueCodes() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    CreateUrlRequest request = CreateUrlRequest.builder()
                            .originalUrl("https://example.com/concurrent-" + threadNum)
                            .build();

                    ResponseEntity<UrlResponse> response = restTemplate.postForEntity(
                            baseUrl,
                            request,
                            UrlResponse.class
                    );

                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                        assertThat(response.getBody().getShortCode()).isNotBlank();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);

        // Verify all short codes are unique
        List<UrlEntity> allUrls = urlRepository.findAll();
        assertThat(allUrls).hasSize(threadCount);
        assertThat(allUrls.stream().map(UrlEntity::getShortCode).distinct().count())
                .isEqualTo(threadCount);
    }

    // =========================================================================
    // ADDITIONAL ENDPOINT COVERAGE
    // =========================================================================

    @Test
    void getAllUrls_returnsPaginatedResults() {
        // Create multiple URLs
        for (int i = 0; i < 5; i++) {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .originalUrl("https://example.com/page" + i)
                    .build();
            restTemplate.postForEntity(baseUrl, request, UrlResponse.class);
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "?page=0&size=3&sort=createdAt,desc",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("page");
        assertThat(response.getBody()).contains("content");
    }

    @Test
    void searchUrls_returnsMatchingResults() {
        CreateUrlRequest request1 = CreateUrlRequest.builder()
                .originalUrl("https://example.com/searchable")
                .build();
        restTemplate.postForEntity(baseUrl, request1, UrlResponse.class);

        CreateUrlRequest request2 = CreateUrlRequest.builder()
                .originalUrl("https://other.com/different")
                .build();
        restTemplate.postForEntity(baseUrl, request2, UrlResponse.class);

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/search?q=searchable",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("searchable");
    }

    @Test
    void dashboardSummary_returnsAggregatedStats() {
        // Create some URLs with clicks
        for (int i = 0; i < 3; i++) {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .originalUrl("https://example.com/dashboard" + i)
                    .build();
            ResponseEntity<UrlResponse> response = restTemplate.postForEntity(baseUrl, request, UrlResponse.class);
            String shortCode = response.getBody().getShortCode();

            // Simulate some clicks
            for (int j = 0; j <= i; j++) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "text/html");
                restTemplate.exchange(baseUrl + "/" + shortCode, HttpMethod.GET, new HttpEntity<>(headers), Void.class);
            }
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/dashboard/summary",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("totalUrls");
        assertThat(response.getBody()).contains("totalClicks");
    }

    @Test
    void dashboardTopUrls_returnsTopClickedUrls() {
        // Create URLs with different click counts
        String[] codes = new String[3];
        for (int i = 0; i < 3; i++) {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .originalUrl("https://example.com/top" + i)
                    .build();
            ResponseEntity<UrlResponse> response = restTemplate.postForEntity(baseUrl, request, UrlResponse.class);
            codes[i] = response.getBody().getShortCode();

            // Click each URL different number of times
            for (int j = 0; j <= i * 2; j++) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "text/html");
                restTemplate.exchange(baseUrl + "/" + codes[i], HttpMethod.GET, new HttpEntity<>(headers), Void.class);
            }
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/dashboard/top?limit=2",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("shortCode");
        assertThat(response.getBody()).contains("clickCount");
    }

    @Test
    void dashboardRecentUrls_returnsRecentlyCreated() {
        for (int i = 0; i < 5; i++) {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .originalUrl("https://example.com/recent" + i)
                    .build();
            restTemplate.postForEntity(baseUrl, request, UrlResponse.class);
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/dashboard/recent?limit=3",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("shortCode");
        assertThat(response.getBody()).contains("originalUrl");
    }

    @Test
    void healthEndpoint_returnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}