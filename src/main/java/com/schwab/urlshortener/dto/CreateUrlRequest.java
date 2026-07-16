package com.schwab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "Original URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^(https?)://[^\s/$.?#].*[^\s]*$",
            message = "Original URL must be a valid HTTP or HTTPS URL"
    )
    @Schema(description = "The original long URL to shorten", example = "https://www.google.com")
    private String originalUrl;

    @Size(max = 64, message = "Custom alias must not exceed 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Custom alias must contain only letters, numbers, underscores, or hyphens")
    @Schema(description = "Optional custom alias for the short URL", example = "google")
    private String customAlias;

    @Min(value = 1, message = "Expiry days must be at least 1")
    @Schema(description = "Optional expiry window in days. Defaults to 30 days when not provided", example = "30", defaultValue = "30")
    private Integer expiryDays = 30;
}
