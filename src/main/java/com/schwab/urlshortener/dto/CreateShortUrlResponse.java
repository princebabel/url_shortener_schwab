package com.schwab.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShortUrlResponse {

    private String alias;
    private String shortUrl;
    private String targetUrl;
}
