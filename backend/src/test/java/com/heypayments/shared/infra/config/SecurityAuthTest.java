package com.heypayments.shared.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.heypayments.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Security authentication and authorization verification per docs/reviews/REVIEW-phase-0.md. */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityAuthTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName(
      "should not generate in-memory security user or print default password to startup logs")
  void shouldNotGenerateDefaultSecurityPassword(CapturedOutput output) {
    assertThat(output.getAll()).doesNotContain("Using generated security password");
  }

  @Test
  @DisplayName(
      "GET unauthenticated protected endpoint should return 401 Problem Details with code UNAUTHENTICATED")
  void shouldReturnProblemDetailsOnUnauthenticated() throws Exception {
    mockMvc
        .perform(get("/api/v1/protected-resource"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthenticated"))
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/unauthenticated"));
  }

  @Test
  @DisplayName("should reject HTTP Basic authentication and return 401 Problem Details")
  void shouldRejectHttpBasicAuth() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/protected-resource").header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
