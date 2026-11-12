package com.heypayments.shared.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.infra.security.RateLimitingFilter;
import com.heypayments.auth.web.dto.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class RateLimitingFilterTest extends BaseIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private RateLimitingFilter rateLimitingFilter;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    rateLimitingFilter.reset();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(rateLimitingFilter).build();
  }

  @AfterEach
  void tearDown() {
    rateLimitingFilter.reset();
  }

  @Test
  @DisplayName(
      "should enforce 5 requests per minute limit on login endpoint and return 429 RATE_LIMITED")
  void shouldEnforceRateLimitOnLogin() throws Exception {
    String phone = "+919999988888";
    LoginRequest req = new LoginRequest(phone, "WrongPassword123!");
    String body = objectMapper.writeValueAsString(req);

    // Make 5 requests (allowed)
    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
    }

    // 6th request must be rate limited with 429
    mockMvc
        .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
  }
}
