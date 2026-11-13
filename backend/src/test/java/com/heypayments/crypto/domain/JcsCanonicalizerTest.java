package com.heypayments.crypto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JcsCanonicalizerTest {

  private final JcsCanonicalizer jcs = new JcsCanonicalizer();

  @Test
  @DisplayName("should canonicalize object by sorting keys lexicographically")
  void shouldSortObjectKeys() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("z", 1);
    map.put("a", "hello");
    map.put("m", true);

    String canonical = jcs.canonicalize(map);
    assertThat(canonical).isEqualTo("{\"a\":\"hello\",\"m\":true,\"z\":1}");
  }

  @Test
  @DisplayName("should canonicalize nested objects and arrays preserving array order")
  void shouldCanonicalizeNestedStructures() {
    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("y", 2);
    inner.put("x", 1);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("b", List.of("banana", "apple"));
    root.put("a", inner);

    String canonical = jcs.canonicalize(root);
    assertThat(canonical).isEqualTo("{\"a\":{\"x\":1,\"y\":2},\"b\":[\"banana\",\"apple\"]}");
  }

  @Test
  @DisplayName("should properly escape control characters and quotes per RFC 8785")
  void shouldEscapeSpecialCharacters() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "Line 1\nLine 2\tTab \"Quotes\" \\ Backslash");

    String canonical = jcs.canonicalize(map);
    assertThat(canonical)
        .isEqualTo("{\"text\":\"Line 1\\nLine 2\\tTab \\\"Quotes\\\" \\\\ Backslash\"}");
  }

  @Test
  @DisplayName("should canonicalize raw JSON string to identical canonical string")
  void shouldCanonicalizeRawJsonString() {
    String inputJson = " {  \"b\":  [ 1 , 2 ] ,  \"a\" : { \"k2\" : true, \"k1\" : null } } ";
    String canonical = jcs.canonicalizeJsonString(inputJson);
    assertThat(canonical).isEqualTo("{\"a\":{\"k1\":null,\"k2\":true},\"b\":[1,2]}");
  }
}
