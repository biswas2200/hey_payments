package com.heypayments.shared.infra.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.heypayments.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies actuator and swagger security constraints per docs/06-api-spec.md and docs/08. */
@AutoConfigureMockMvc
class ActuatorSecurityTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("should return 401 Unauthorized when unauthenticated accessing /actuator/env")
  void shouldReturn401ForUnauthenticatedActuatorEnv() throws Exception {
    mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName(
      "should allow anonymous access to /actuator/health without leaking component details")
  void shouldAllowAnonymousHealthWithoutComponentDetails() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  @DisplayName("should deny unauthenticated access to Swagger docs in non-dev profile")
  void shouldDenySwaggerDocsInNonDevProfile() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
  }
}
