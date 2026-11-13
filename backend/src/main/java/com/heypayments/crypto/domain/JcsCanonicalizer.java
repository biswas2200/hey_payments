package com.heypayments.crypto.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.erdtman.jcs.JsonCanonicalizer;

/**
 * Implementation of RFC 8785 JSON Canonicalization Scheme (JCS). Produces deterministic, canonical
 * UTF-8 JSON byte streams for cryptographic signing.
 */
public class JcsCanonicalizer {

  private final ObjectMapper objectMapper;

  public JcsCanonicalizer() {
    this.objectMapper = new ObjectMapper();
  }

  public JcsCanonicalizer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String canonicalize(Object object) {
    try {
      String rawJson = objectMapper.writeValueAsString(object);
      return canonicalizeJsonString(rawJson);
    } catch (CryptoException e) {
      throw e;
    } catch (Exception e) {
      throw new CryptoException("Failed to canonicalize object to JCS", e);
    }
  }

  public String canonicalizeJsonString(String json) {
    try {
      JsonCanonicalizer jc = new JsonCanonicalizer(json);
      return jc.getEncodedString();
    } catch (Exception e) {
      throw new CryptoException("Failed to canonicalize JSON string per RFC 8785", e);
    }
  }

  public byte[] canonicalizeToBytes(Object object) {
    return canonicalize(object).getBytes(StandardCharsets.UTF_8);
  }
}
