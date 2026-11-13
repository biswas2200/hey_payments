package com.heypayments.shared.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health check controller exposing basic service status. */
@RestController
@RequestMapping("/api")
public class HealthController {

  /**
   * Returns the service health status and current timestamp.
   *
   * @return response entity with status map
   */
  @GetMapping("/health")
  @PreAuthorize("permitAll()")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    return ResponseEntity.ok(
        Map.of(
            "status", "UP",
            "service", "Hey Payments Backend",
            "timestamp", Instant.now().toString()));
  }
}
