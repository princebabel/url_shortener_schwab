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
@Table(name = "urls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 64)
    private String shortCode;

    @Column(unique = true, length = 64)
    private String customAlias;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private Integer clickCount;

    private Instant lastAccessedAt;

    @Column(nullable = false)
    private Boolean active;
}
