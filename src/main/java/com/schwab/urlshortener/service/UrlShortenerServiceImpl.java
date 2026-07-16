package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    @Override
    public BaseResponse<String> health() {
        log.info("Health check requested");
        return BaseResponse.success("Service is up and running", "UP");
    }
}
