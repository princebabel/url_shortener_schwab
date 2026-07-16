package com.schwab.urlshortener.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTypeCoverageTest {

    @Test
    void exceptionTypesExposeMessagesAndCause() {
        InvalidUrlException invalidUrlException = new InvalidUrlException("invalid");
        DuplicateAliasException duplicateAliasException = new DuplicateAliasException("duplicate");
        ExpiredUrlException expiredUrlException = new ExpiredUrlException("expired");
        InactiveUrlException inactiveUrlException = new InactiveUrlException("inactive");
        ShortCodeNotFoundException shortCodeNotFoundException = new ShortCodeNotFoundException("missing");
        DatabaseException databaseException = new DatabaseException("db failure");

        assertThat(invalidUrlException.getMessage()).isEqualTo("invalid");
        assertThat(duplicateAliasException.getMessage()).isEqualTo("duplicate");
        assertThat(expiredUrlException.getMessage()).isEqualTo("expired");
        assertThat(inactiveUrlException.getMessage()).isEqualTo("inactive");
        assertThat(shortCodeNotFoundException.getMessage()).isEqualTo("missing");
        assertThat(databaseException.getMessage()).isEqualTo("db failure");
    }
}
