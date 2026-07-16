package com.schwab.urlshortener.mapper;

import com.schwab.urlshortener.dto.CreateShortUrlResponse;
import com.schwab.urlshortener.entity.UrlLinkEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlLinkMapperTest {

    private final UrlLinkMapper mapper = new UrlLinkMapper();

    @Test
    void toResponse_mapsAliasTargetUrlAndBaseUrlIntoResponse() {
        UrlLinkEntity entity = UrlLinkEntity.builder()
                .alias("my-alias")
                .targetUrl("https://example.com")
                .build();

        CreateShortUrlResponse response = mapper.toResponse(entity, "https://short.ly");

        assertThat(response.getAlias()).isEqualTo("my-alias");
        assertThat(response.getTargetUrl()).isEqualTo("https://example.com");
        assertThat(response.getShortUrl()).isEqualTo("https://short.ly/my-alias");
    }

    @Test
    void toResponse_concatenatesBaseUrlAndAliasExactlyAsProvided() {
        UrlLinkEntity entity = UrlLinkEntity.builder()
                .alias("demo")
                .targetUrl("https://target.example")
                .build();

        CreateShortUrlResponse response = mapper.toResponse(entity, "https://short.ly/");

        assertThat(response.getShortUrl()).isEqualTo("https://short.ly//demo");
    }
}
