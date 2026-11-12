package com.heypayments.shared.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("should create Money from paise")
    void shouldCreateFromPaise() {
        Money money = Money.ofPaise(2500L);
        assertThat(money.paise()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should create Money from rupees")
    void shouldCreateFromRupees() {
        Money money = Money.ofRupees(25L);
        assertThat(money.paise()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should create zero Money")
    void shouldCreateZero() {
        Money zero = Money.zero();
        assertThat(zero.paise()).isZero();
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    @DisplayName("should reject negative paise amount")
    void shouldRejectNegativePaise() {
        assertThatThrownBy(() -> Money.ofPaise(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("should add two Money instances")
    void shouldAddMoney() {
        Money m1 = Money.ofPaise(1000L);
        Money m2 = Money.ofPaise(2500L);
        Money result = m1.plus(m2);

        assertThat(result.paise()).isEqualTo(3500L);
    }

    @Test
    @DisplayName("should subtract Money instances when minuend >= subtrahend")
    void shouldSubtractMoney() {
        Money m1 = Money.ofPaise(3500L);
        Money m2 = Money.ofPaise(1000L);
        Money result = m1.minus(m2);

        assertThat(result.paise()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should throw when subtracting more money than available")
    void shouldThrowWhenSubtractionExceeds() {
        Money m1 = Money.ofPaise(1000L);
        Money m2 = Money.ofPaise(2000L);

        assertThatThrownBy(() -> m1.minus(m2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("should multiply Money by an integer factor")
    void shouldMultiplyMoney() {
        Money m = Money.ofPaise(150L);
        Money result = m.multiply(3);

        assertThat(result.paise()).isEqualTo(450L);
    }

    @Test
    @DisplayName("should reject negative multiplier")
    void shouldRejectNegativeMultiplier() {
        Money m = Money.ofPaise(150L);

        assertThatThrownBy(() -> m.multiply(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiplier cannot be negative");
    }

    @Test
    @DisplayName("should correctly compare Money instances")
    void shouldCompareMoney() {
        Money m1 = Money.ofPaise(100L);
        Money m2 = Money.ofPaise(200L);
        Money m3 = Money.ofPaise(100L);

        assertThat(m1.isLessThan(m2)).isTrue();
        assertThat(m2.isGreaterThan(m1)).isTrue();
        assertThat(m1.isGreaterThanOrEqual(m3)).isTrue();
        assertThat(m1.compareTo(m2)).isNegative();
        assertThat(m2.compareTo(m1)).isPositive();
        assertThat(m1.compareTo(m3)).isZero();
    }

    @Test
    @DisplayName("should format Money in INR currency notation")
    void shouldFormatInrNotation() {
        assertThat(Money.ofPaise(0L).format()).isEqualTo("₹0.00");
        assertThat(Money.ofPaise(50L).format()).isEqualTo("₹0.50");
        assertThat(Money.ofPaise(100L).format()).isEqualTo("₹1.00");
        assertThat(Money.ofPaise(200000L).format()).isEqualTo("₹2,000.00");
    }
}
