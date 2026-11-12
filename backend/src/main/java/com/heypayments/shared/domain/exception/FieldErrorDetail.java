package com.heypayments.shared.domain.exception;

/** Details of an individual input field validation error. */
public record FieldErrorDetail(String field, String message) {}
