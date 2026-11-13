package com.heypayments.crypto.domain.model;

/**
 * Key purpose classification per doc 08 security rules. Separates protocol signing keys from JWT
 * authentication keys.
 */
public enum ServerKeyPurpose {
  PROTOCOL,
  JWT
}
