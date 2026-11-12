package com.heypayments.shared.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable value object representing monetary amounts in integer paise (1 INR = 100 paise).
 * Floating-point representation of money is strictly prohibited across the codebase.
 *
 * @param paise the amount in integer minor units (paise)
 */
public record Money(long paise) implements Comparable<Money> {

  /** Compact constructor validating the non-negative invariant. */
  public Money {
    if (paise < 0) {
      throw new IllegalArgumentException("Money amount in paise cannot be negative: " + paise);
    }
  }

  /**
   * Factory method creating a Money instance from integer paise.
   *
   * @param paise the amount in paise
   * @return non-null Money instance
   */
  public static Money ofPaise(long paise) {
    return new Money(paise);
  }

  /**
   * Factory method creating a Money instance from rupees (1 INR = 100 paise).
   *
   * @param rupees the amount in whole rupees
   * @return non-null Money instance
   */
  public static Money ofRupees(long rupees) {
    if (rupees < 0) {
      throw new IllegalArgumentException("Rupees amount cannot be negative: " + rupees);
    }
    return new Money(Math.multiplyExact(rupees, 100L));
  }

  /**
   * Factory method creating a zero Money instance.
   *
   * @return zero Money instance
   */
  public static Money zero() {
    return new Money(0L);
  }

  /**
   * Adds another Money amount to this instance.
   *
   * @param other the addend
   * @return sum Money instance
   */
  public Money plus(Money other) {
    Objects.requireNonNull(other, "Cannot add null Money");
    return new Money(Math.addExact(this.paise, other.paise));
  }

  /**
   * Subtracts another Money amount from this instance.
   *
   * @param other the subtrahend
   * @return difference Money instance
   */
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

  /**
   * Multiplies this Money amount by an integer factor.
   *
   * @param factor the multiplication factor
   * @return multiplied Money instance
   */
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

  /**
   * Formats this Money instance as a formatted INR string (e.g., ₹2,000.00).
   *
   * @return formatted INR currency string
   */
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
