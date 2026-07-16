package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.BaseResponse;

public interface UrlShortenerService {

    BaseResponse<String> health();
}
