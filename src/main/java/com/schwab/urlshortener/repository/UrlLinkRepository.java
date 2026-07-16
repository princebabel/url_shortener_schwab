package com.schwab.urlshortener.repository;

import com.schwab.urlshortener.entity.UrlLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlLinkRepository extends JpaRepository<UrlLinkEntity, Long> {

    Optional<UrlLinkEntity> findByAlias(String alias);

    boolean existsByAlias(String alias);
}
