package com.schwab.urlshortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "url_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String alias;

    @Column(nullable = false, length = 2048)
    private String targetUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    private Instant lastAccessedAt;

    @Column(nullable = false)
    private long accessCount;
}
