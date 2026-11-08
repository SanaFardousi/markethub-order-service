package com.markethub.orders.unit;

import com.markethub.orders.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


class MoneyTest {

    @ParameterizedTest(name = "{0} becomes {1}")
    @CsvSource({ "1.2344, 1.234", "1.2345, 1.235", "1.2346, 1.235", "0.0004, 0.000",
        "0.0005, 0.001", "5, 5.000", "12.5, 12.500", "-0.0005, -0.001" })
    @DisplayName("an amount is rounded half up to exactly three decimals")
    void money_shouldRoundHalfUpToThreeDecimals(BigDecimal input, String expected) {
        assertThat(new Money(input).amount().toPlainString()).isEqualTo(expected);
    }

    @Test
    void money_shouldTreatTrailingZerosAsTheSameAmount() {
        assertThat(new Money(new BigDecimal("1.5"))).isEqualTo(new Money(new BigDecimal("1.500")));
    }

    @Test
    void zero_shouldHaveThreeDecimals() {
        assertThat(Money.ZERO.amount().toPlainString()).isEqualTo("0.000");
    }
}
