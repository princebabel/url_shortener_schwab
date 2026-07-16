package com.schwab.urlshortener.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidatorTest {

    private final UrlValidator urlValidator = new UrlValidator();

    @Test
    void isValid_whenUrlIsNull_returnsFalse() {
        assertThat(urlValidator.isValid(null)).isFalse();
    }

    @Test
    void isValid_whenUrlIsBlank_returnsFalse() {
        assertThat(urlValidator.isValid("   ")).isFalse();
    }

    @Test
    void isValid_whenUrlHasSchemeAndHost_returnsTrue() {
        assertThat(urlValidator.isValid("https://example.com")).isTrue();
    }

    @Test
    void isValid_whenUrlUsesFtpSchemeButHasHost_returnsTrue() {
        assertThat(urlValidator.isValid("ftp://example.com")).isTrue();
    }

    @Test
    void isValid_whenUriHasNoHost_returnsFalse() {
        assertThat(urlValidator.isValid("https://")).isFalse();
    }

    @Test
    void isValid_whenUriCannotBeParsed_returnsFalse() {
        assertThat(urlValidator.isValid("not a valid uri")).isFalse();
    }
}
