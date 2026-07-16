package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.CreateUrlRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlValidator urlValidator;

    @InjectMocks
    private UrlServiceImpl urlService;

    @Test
    void createUrl_whenOriginalUrlIsInvalid_throwsInvalidUrlException() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("not-a-url")
                .build();

        when(urlValidator.isValid("not-a-url")).thenReturn(false);

        assertThatThrownBy(() -> urlService.createUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("valid HTTP or HTTPS URL");

        verifyNoInteractions(urlRepository);
    }

    @Test
    void createUrl_whenCustomAliasAlreadyExists_throwsDuplicateAliasException() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("existing")
                .build();

        when(urlValidator.isValid("https://example.com")).thenReturn(true);
        when(urlRepository.existsByCustomAlias("existing")).thenReturn(true);

        assertThatThrownBy(() -> urlService.createUrl(request))
                .isInstanceOf(DuplicateAliasException.class)
                .hasMessageContaining("already exists");

        verify(urlRepository).existsByCustomAlias("existing");
    }

    @Test
    void createUrl_whenValidRequest_returnsResponse() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .build();

        when(urlValidator.isValid("https://example.com")).thenReturn(true);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.saveAndFlush(any(UrlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponse response = urlService.createUrl(request);

        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(response.getShortCode()).isNotBlank();
        assertThat(response.getActive()).isTrue();

        ArgumentCaptor<UrlEntity> captor = ArgumentCaptor.forClass(UrlEntity.class);
        verify(urlRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOriginalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void redirect_whenUrlIsActiveAndNotExpired_returnsOriginalUrl() {
        UrlEntity entity = UrlEntity.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(true)
                .expiryDate(Instant.now().plusSeconds(60))
                .clickCount(0)
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));
        when(urlRepository.incrementClickCount(anyString(), any(Instant.class))).thenReturn(1);

        String targetUrl = urlService.redirect("abc123");

        assertThat(targetUrl).isEqualTo("https://example.com");
        verify(urlRepository).incrementClickCount(anyString(), any(Instant.class));
    }

    @Test
    void redirect_whenUrlIsExpired_throwsExpiredUrlException() {
        UrlEntity entity = UrlEntity.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(true)
                .expiryDate(Instant.now().minusSeconds(60))
                .clickCount(0)
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> urlService.redirect("abc123"))
                .isInstanceOf(ExpiredUrlException.class);
    }

    @Test
    void redirect_whenUrlIsInactive_throwsInactiveUrlException() {
        UrlEntity entity = UrlEntity.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(false)
                .expiryDate(Instant.now().plusSeconds(60))
                .clickCount(0)
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> urlService.redirect("abc123"))
                .isInstanceOf(InactiveUrlException.class);
    }

    @Test
    void redirect_whenShortCodeDoesNotExist_throwsShortCodeNotFoundException() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.redirect("missing"))
                .isInstanceOf(ShortCodeNotFoundException.class);
    }

    @Test
    void getAnalytics_whenShortCodeExists_returnsAnalyticsResponse() {
        UrlEntity entity = UrlEntity.builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .clickCount(4)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiryDate(Instant.parse("2024-02-01T00:00:00Z"))
                .active(true)
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));

        UrlAnalyticsResponse response = urlService.getAnalytics("abc123");

        assertThat(response.getShortCode()).isEqualTo("abc123");
        assertThat(response.getClickCount()).isEqualTo(4);
        assertThat(response.getActive()).isTrue();
    }
}
