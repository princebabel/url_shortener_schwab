package com.schwab.urlshortener.mapper;

import com.schwab.urlshortener.dto.CreateShortUrlResponse;
import com.schwab.urlshortener.entity.UrlLinkEntity;
import org.springframework.stereotype.Component;

@Component
public class UrlLinkMapper {

    public CreateShortUrlResponse toResponse(UrlLinkEntity entity, String baseUrl) {
        return CreateShortUrlResponse.builder()
                .alias(entity.getAlias())
                .shortUrl(baseUrl + "/" + entity.getAlias())
                .targetUrl(entity.getTargetUrl())
                .build();
    }
}
