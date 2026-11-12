package com.heypayments.crypto.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of RFC 8785 JSON Canonicalization Scheme (JCS).
 * Produces deterministic, canonical UTF-8 JSON byte streams for cryptographic signing.
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
      JsonNode node = objectMapper.valueToTree(object);
      StringBuilder sb = new StringBuilder();
      canonicalizeNode(node, sb);
      return sb.toString();
    } catch (Exception e) {
      throw new CryptoException("Failed to canonicalize object to JCS", e);
    }
  }

  public String canonicalizeJsonString(String json) {
    try {
      JsonNode node = objectMapper.readTree(json);
      StringBuilder sb = new StringBuilder();
      canonicalizeNode(node, sb);
      return sb.toString();
    } catch (JsonProcessingException e) {
      throw new CryptoException("Failed to parse JSON string for canonicalization", e);
    }
  }

  public byte[] canonicalizeToBytes(Object object) {
    return canonicalize(object).getBytes(StandardCharsets.UTF_8);
  }

  private void canonicalizeNode(JsonNode node, StringBuilder sb) {
    if (node == null || node.isNull()) {
      sb.append("null");
    } else if (node.isBoolean()) {
      sb.append(node.asBoolean() ? "true" : "false");
    } else if (node.isNumber()) {
      if (node.isIntegralNumber()) {
        sb.append(node.asText());
      } else {
        // Double or Float: format per ECMAScript specifications
        double val = node.asDouble();
        if (val == (long) val) {
          sb.append((long) val);
        } else {
          sb.append(node.asText());
        }
      }
    } else if (node.isTextual()) {
      escapeString(node.asText(), sb);
    } else if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      sb.append('[');
      for (int i = 0; i < arrayNode.size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        canonicalizeNode(arrayNode.get(i), sb);
      }
      sb.append(']');
    } else if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      List<String> keys = new ArrayList<>();
      Iterator<String> fieldNames = objectNode.fieldNames();
      while (fieldNames.hasNext()) {
        keys.add(fieldNames.next());
      }
      // RFC 8785 §3.2.3: keys must be sorted by UTF-16 code units (lexicographical)
      Collections.sort(keys);

      sb.append('{');
      for (int i = 0; i < keys.size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        String key = keys.get(i);
        escapeString(key, sb);
        sb.append(':');
        canonicalizeNode(objectNode.get(key), sb);
      }
      sb.append('}');
    }
  }

  private void escapeString(String s, StringBuilder sb) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }
}
