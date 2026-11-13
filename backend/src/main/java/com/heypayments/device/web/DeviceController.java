package com.heypayments.device.web;

import com.heypayments.device.application.DeviceService;
import com.heypayments.device.web.dto.CrlResponse;
import com.heypayments.device.web.dto.DeviceResponse;
import com.heypayments.device.web.dto.RegisterDeviceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "devices", description = "Device registration, keys, and revocation list")
public class DeviceController {

  private final DeviceService deviceService;

  @PostMapping
  @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
  @Operation(
      summary = "Register device with public key and receive merchant certificate if merchant")
  public ResponseEntity<DeviceResponse> registerDevice(
      @Valid @RequestBody RegisterDeviceRequest request, Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    String roleName =
        authentication.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .filter(r -> r.equals("PAYER") || r.equals("MERCHANT"))
            .findFirst()
            .orElse("PAYER");

    DeviceResponse response = deviceService.registerDevice(userId, roleName, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current active device for authenticated user")
  public ResponseEntity<DeviceResponse> getActiveDevice(Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    return deviceService
        .findActiveDevice(userId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Close active device")
  public ResponseEntity<Void> closeDevice(@PathVariable("id") UUID deviceId, Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    deviceService.closeDevice(userId, deviceId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/crl")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get certificate revocation list delta since timestamp")
  public ResponseEntity<CrlResponse> getCrl(
      @RequestParam(name = "since", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant since) {
    return ResponseEntity.ok(deviceService.getCrl(since));
  }
}
