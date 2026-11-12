package com.heypayments.auth.web;

import com.heypayments.auth.application.AuthService;
import com.heypayments.auth.web.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "auth", description = "User profile endpoints")
public class UserController {

  private final AuthService authService;

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get currently authenticated user profile")
  public ResponseEntity<UserProfileResponse> getCurrentUser(Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    return ResponseEntity.ok(authService.getProfile(userId));
  }
}
