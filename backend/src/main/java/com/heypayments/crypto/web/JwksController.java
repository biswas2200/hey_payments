package com.heypayments.crypto.web;

import com.heypayments.crypto.application.ServerKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Tag(name = "devices", description = "Server public key discovery and JWKS endpoint")
public class JwksController {

  private final ServerKeyService serverKeyService;

  @GetMapping("/jwks")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Get server public keys in JWKS format")
  public ResponseEntity<Map<String, Object>> getJwks() {
    return ResponseEntity.ok(serverKeyService.getJwks());
  }
}
