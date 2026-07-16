package com.schwab.urlshortener.repository;

import com.schwab.urlshortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    @Modifying
    @Query("update UrlEntity u set u.clickCount = u.clickCount + 1, u.lastAccessedAt = :lastAccessedAt where u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode, @Param("lastAccessedAt") Instant lastAccessedAt);

    Page<UrlEntity> findByOriginalUrlContainingIgnoreCaseOrShortCodeContainingIgnoreCaseOrCustomAliasContainingIgnoreCase(
            String originalUrl,
            String shortCode,
            String customAlias,
            Pageable pageable);

    Page<UrlEntity> findByActive(Boolean active, Pageable pageable);

    Page<UrlEntity> findByActiveAndExpiryDateBefore(Boolean active, Instant expiryDate, Pageable pageable);

    Page<UrlEntity> findAllByOrderByClickCountDesc(Pageable pageable);

    Page<UrlEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByActiveTrue();

    long countByExpiryDateBefore(Instant now);

    long count();

    @Query("select coalesce(sum(u.clickCount), 0) from UrlEntity u")
    long sumClickCount();

    @Query("select u from UrlEntity u where lower(u.originalUrl) like lower(concat('%', :query, '%')) or lower(u.shortCode) like lower(concat('%', :query, '%')) or lower(coalesce(u.customAlias, '')) like lower(concat('%', :query, '%'))")
    Page<UrlEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
