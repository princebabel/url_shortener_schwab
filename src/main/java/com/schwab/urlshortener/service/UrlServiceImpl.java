package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.DashboardSummaryResponse;
import com.schwab.urlshortener.dto.UrlAnalyticsResponse;
import com.schwab.urlshortener.dto.UrlResponse;
import com.schwab.urlshortener.entity.UrlEntity;
import com.schwab.urlshortener.exception.DuplicateAliasException;
import com.schwab.urlshortener.exception.ExpiredUrlException;
import com.schwab.urlshortener.exception.InactiveUrlException;
import com.schwab.urlshortener.exception.InvalidUrlException;
import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.repository.UrlRepository;
import com.schwab.urlshortener.validation.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UrlValidator urlValidator;

    @Override
    @Transactional
    public UrlResponse createUrl(CreateUrlRequest request) {
        if (!urlValidator.isValid(request.getOriginalUrl())) {
            throw new InvalidUrlException("Original URL must be a valid HTTP or HTTPS URL");
        }

        String customAlias = request.getCustomAlias();
        if (customAlias != null && !customAlias.isBlank()) {
            if (urlRepository.existsByCustomAlias(customAlias) || urlRepository.existsByShortCode(customAlias)) {
                throw new DuplicateAliasException("Custom alias already exists");
            }
        }

        String shortCode = generateShortCode(customAlias);
        Instant now = Instant.now();
        int expiryDays = request.getExpiryDays() != null ? request.getExpiryDays() : 30;
        Instant expiryDate = now.plus(expiryDays, ChronoUnit.DAYS);

        UrlEntity entity = UrlEntity.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .customAlias(customAlias != null && !customAlias.isBlank() ? customAlias : null)
                .createdAt(now)
                .expiryDate(expiryDate)
                .clickCount(0)
                .active(true)
                .build();

        try {
            UrlEntity savedEntity = urlRepository.saveAndFlush(entity);
            if (log.isInfoEnabled()) {
                log.info("Created short URL with code {}", savedEntity.getShortCode());
            }
            return toResponse(savedEntity);
        } catch (DataAccessException ex) {
            if (log.isErrorEnabled()) {
                log.error("Database error while creating URL", ex);
            }
            // Translate unique constraint / integrity violations into a DuplicateAliasException
            if (ex instanceof DataIntegrityViolationException || (ex.getCause() != null && ex.getCause().getMessage() != null && ex.getCause().getMessage().toLowerCase().contains("unique"))) {
                throw new DuplicateAliasException("Custom alias already exists");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public String redirect(String shortCode) {
        log.info("Redirect requested for short code {}", shortCode);

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found"));

        Instant now = Instant.now();
        if (!Boolean.TRUE.equals(entity.getActive())) {
            log.warn("Redirect blocked for inactive short code {}", shortCode);
            throw new InactiveUrlException("URL is inactive");
        }

        if (entity.getExpiryDate() != null && entity.getExpiryDate().isBefore(now)) {
            log.warn("Redirect blocked for expired short code {}", shortCode);
            throw new ExpiredUrlException("URL has expired");
        }

        int updatedRows = urlRepository.incrementClickCount(shortCode, now);
        if (updatedRows != 1) {
            if (log.isWarnEnabled()) {
                log.warn("Click tracking update did not affect one row for short code {}", shortCode);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Redirecting short code {} to {}", shortCode, entity.getOriginalUrl());
        }
        return entity.getOriginalUrl();
    }

    @Override
    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found"));

        Instant now = Instant.now();
        if (!Boolean.TRUE.equals(entity.getActive())) {
            throw new InactiveUrlException("URL is inactive");
        }
        // Analytics should be available for inspection even after a URL has expired.
        // Only block analytics for explicitly inactive URLs.

        return UrlAnalyticsResponse.builder()
            .shortCode(entity.getShortCode())
            .originalUrl(entity.getOriginalUrl())
            .clickCount(entity.getClickCount())
            .createdAt(entity.getCreatedAt())
            .expiryDate(entity.getExpiryDate())
            .lastAccessedAt(entity.getLastAccessedAt())
            .active(entity.getActive())
            .build();
    }

    @Override
    public Page<UrlResponse> getUrls(String search, Boolean active, Boolean expired, Pageable pageable) {
        Instant now = Instant.now();
        if (search != null && !search.isBlank()) {
            return urlRepository.searchByQuery(search, pageable).map(this::toResponse);
        }
        if (Boolean.TRUE.equals(active) && Boolean.TRUE.equals(expired)) {
            return urlRepository.findAll(pageable).map(this::toResponse);
        }
        if (Boolean.TRUE.equals(active)) {
            return urlRepository.findByActive(true, pageable).map(this::toResponse);
        }
        if (Boolean.TRUE.equals(expired)) {
            return urlRepository.findByActiveAndExpiryDateBefore(false, now, pageable).map(this::toResponse);
        }
        return urlRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        long totalUrls = urlRepository.count();
        long activeUrls = urlRepository.countByActiveTrue();
        long expiredUrls = urlRepository.countByExpiryDateBefore(Instant.now());
        long totalClicks = urlRepository.sumClickCount();
        double averageClicksPerUrl = totalUrls == 0 ? 0 : (double) totalClicks / totalUrls;
        double activePercentage = totalUrls == 0 ? 0 : (double) activeUrls / totalUrls * 100;
        double expiredPercentage = totalUrls == 0 ? 0 : (double) expiredUrls / totalUrls * 100;

        String topShortCode = urlRepository.findAll(Pageable.ofSize(1)).stream()
                .findFirst()
                .map(UrlEntity::getShortCode)
                .orElse(null);

        return DashboardSummaryResponse.builder()
                .totalUrls(totalUrls)
                .activeUrls(activeUrls)
                .expiredUrls(expiredUrls)
                .totalClicks(totalClicks)
                .topShortCode(topShortCode)
                .averageClicksPerUrl(averageClicksPerUrl)
                .activePercentage(activePercentage)
                .expiredPercentage(expiredPercentage)
                .build();
    }

    @Override
    public Page<UrlResponse> getTopUrls(Pageable pageable) {
        return urlRepository.findAllByOrderByClickCountDesc(pageable).map(this::toResponse);
    }

    @Override
    public Page<UrlResponse> getRecentUrls(Pageable pageable) {
        return urlRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    private String generateShortCode(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            return customAlias;
        }

        String candidate;
        do {
            candidate = UUID.randomUUID().toString().substring(0, 8);
        } while (urlRepository.existsByShortCode(candidate));

        return candidate;
    }

    private UrlResponse toResponse(UrlEntity entity) {
        return UrlResponse.builder()
                .id(entity.getId())
                .originalUrl(entity.getOriginalUrl())
                .shortCode(entity.getShortCode())
                .customAlias(entity.getCustomAlias())
                .createdAt(entity.getCreatedAt())
                .expiryDate(entity.getExpiryDate())
                .clickCount(entity.getClickCount())
                .active(entity.getActive())
                .build();
    }
}
