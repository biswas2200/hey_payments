package com.heypayments.shared.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable value object representing monetary amounts in integer paise (1 INR = 100 paise).
 * Floating-point representation of money is strictly prohibited across the codebase.
 */
public record Money(long paise) implements Comparable<Money> {

  public Money {
    if (paise < 0) {
      throw new IllegalArgumentException("Money amount in paise cannot be negative: " + paise);
    }
  }

  public static Money ofPaise(long paise) {
    return new Money(paise);
  }

  public static Money ofRupees(long rupees) {
    if (rupees < 0) {
      throw new IllegalArgumentException("Rupees amount cannot be negative: " + rupees);
    }
    return new Money(Math.multiplyExact(rupees, 100L));
  }

  public static Money zero() {
    return new Money(0L);
  }

  public Money plus(Money other) {
    Objects.requireNonNull(other, "Cannot add null Money");
    return new Money(Math.addExact(this.paise, other.paise));
  }

  public Money minus(Money other) {
    Objects.requireNonNull(other, "Cannot subtract null Money");
    if (this.paise < other.paise) {
      throw new IllegalArgumentException(
          "Subtrahend "
              + other.paise
              + " exceeds minuend "
              + this.paise
              + "; Money cannot be negative.");
    }
    return new Money(this.paise - other.paise);
  }

  public Money multiply(long factor) {
    if (factor < 0) {
      throw new IllegalArgumentException("Multiplier cannot be negative: " + factor);
    }
    return new Money(Math.multiplyExact(this.paise, factor));
  }

  public boolean isZero() {
    return this.paise == 0L;
  }

  public boolean isPositive() {
    return this.paise > 0L;
  }

  public boolean isGreaterThan(Money other) {
    Objects.requireNonNull(other, "Cannot compare with null Money");
    return this.paise > other.paise;
  }

  public boolean isGreaterThanOrEqual(Money other) {
    Objects.requireNonNull(other, "Cannot compare with null Money");
    return this.paise >= other.paise;
  }

  public boolean isLessThan(Money other) {
    Objects.requireNonNull(other, "Cannot compare with null Money");
    return this.paise < other.paise;
  }

  public String format() {
    long rupees = paise / 100L;
    long remainder = paise % 100L;
    return String.format(Locale.US, "₹%,d.%02d", rupees, remainder);
  }

  @Override
  public int compareTo(Money other) {
    Objects.requireNonNull(other, "Cannot compare with null Money");
    return Long.compare(this.paise, other.paise);
  }

  @Override
  public String toString() {
    return format();
  }
}
