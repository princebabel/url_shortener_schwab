package com.schwab.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    private final IdGenerator idGenerator = new IdGenerator();

    @Test
    void generateAlias_defaultLengthReturnsEightCharacters() {
        String alias = idGenerator.generateAlias();

        assertThat(alias).hasSize(8);
        assertThat(alias).matches("[a-z0-9]{8}");
    }

    @Test
    void generateAlias_customLengthReturnsRequestedLength() {
        String alias = idGenerator.generateAlias(12);

        assertThat(alias).hasSize(12);
        assertThat(alias).matches("[a-z0-9]{12}");
    }

    @Test
    void generateAlias_zeroLengthReturnsEmptyString() {
        String alias = idGenerator.generateAlias(0);

        assertThat(alias).isEmpty();
    }
}
