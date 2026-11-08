package com.markethub.orders.it;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextLoadsTest extends AbstractIntegrationTest {

    @Test
    void context_shouldStart_whenAllBeansAreWiredAndRulesCompile() {
        assertThat(countOf("product")).isEqualTo(100);
    }
}
