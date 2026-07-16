package com.schwab.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalUrls;
    private long activeUrls;
    private long expiredUrls;
    private long totalClicks;
    private String topShortCode;
    private double averageClicksPerUrl;
    private double activePercentage;
    private double expiredPercentage;
}
