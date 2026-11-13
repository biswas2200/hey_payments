package com.heypayments.shared.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.application.PhoneRateLimiter;
import com.heypayments.auth.infra.security.RateLimitingFilter;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.shared.web.RequestIdFilter;
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
  @Autowired(required = false) private PhoneRateLimiter phoneRateLimiter;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    rateLimitingFilter.reset();
    if (phoneRateLimiter != null) {
      phoneRateLimiter.reset();
    }
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(new RequestIdFilter(), rateLimitingFilter)
            .build();
  }

  @AfterEach
  void tearDown() {
    rateLimitingFilter.reset();
    if (phoneRateLimiter != null) {
      phoneRateLimiter.reset();
    }
  }

  @Test
  @DisplayName("Two clients with different source IPs do not share a bucket (20/min per IP)")
  void shouldAllowSeparateBucketsForDifferentClientIps() throws Exception {
    String clientIp1 = "198.51.100.1";
    String clientIp2 = "198.51.100.2";

    // Client 1 makes 20 requests (each with a different phone number to isolate IP limit)
    for (int i = 0; i < 20; i++) {
      LoginRequest req = new LoginRequest("+9190000000" + String.format("%02d", i), "Pwd1234567!");
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(request -> {
                    request.setRemoteAddr(clientIp1);
                    return request;
                  })
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
    }

    // 21st request from Client 1 is rate limited (429)
    LoginRequest req21 = new LoginRequest("+919000000099", "Pwd1234567!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(request -> {
                  request.setRemoteAddr(clientIp1);
                  return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req21)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/rate-limited"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());

    // Client 2 with different IP should NOT be blocked
    LoginRequest reqClient2 = new LoginRequest("+919111111111", "Pwd1234567!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(request -> {
                  request.setRemoteAddr(clientIp2);
                  return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqClient2)))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
  }

  @Test
  @DisplayName("Per-phone rate limit triggers across different client IPs (5/min per phone)")
  void shouldEnforcePerPhoneRateLimitAcrossDifferentIps() throws Exception {
    String targetPhone = "+919888877777";
    LoginRequest req = new LoginRequest(targetPhone, "Pwd1234567!");
    String body = objectMapper.writeValueAsString(req);

    // 5 attempts from 5 distinct IP addresses
    for (int i = 1; i <= 5; i++) {
      String ip = "10.0.1." + i;
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                  })
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
    }

    // 6th attempt from a 6th distinct IP address for the same phone must be rate-limited
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(request -> {
                  request.setRemoteAddr("10.0.1.6");
                  return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/rate-limited"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  @DisplayName("A spoofed X-Forwarded-For header from an untrusted remote is ignored")
  void shouldIgnoreSpoofedXForwardedForHeaderFromUntrustedRemote() throws Exception {
    String untrustedRemoteIp = "203.0.113.195";

    // Untrusted remote tries to bypass 20/min limit by rotating spoofed X-Forwarded-For headers
    for (int i = 0; i < 20; i++) {
      LoginRequest req = new LoginRequest("+9192222222" + String.format("%02d", i), "Pwd1234567!");
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(request -> {
                    request.setRemoteAddr(untrustedRemoteIp);
                    return request;
                  })
                  .header("X-Forwarded-For", "198.51.100." + i)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
    }

    // 21st request with yet another spoofed IP must STILL be rate limited because untrusted remote IP is keyed
    LoginRequest req21 = new LoginRequest("+919222222299", "Pwd1234567!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(request -> {
                  request.setRemoteAddr(untrustedRemoteIp);
                  return request;
                })
                .header("X-Forwarded-For", "1.2.3.4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req21)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/rate-limited"));
  }

  @Test
  @DisplayName("429 response contains consistent problem type and requestId")
  void shouldReturnConsistentProblemDetailsWithRequestIdOn429() throws Exception {
    String clientIp = "192.0.2.55";
    for (int i = 0; i < 20; i++) {
      LoginRequest req = new LoginRequest("+9188888000" + String.format("%02d", i), "Pwd1234567!");
      mockMvc.perform(
          post("/api/v1/auth/login")
              .with(request -> {
                request.setRemoteAddr(clientIp);
                return request;
              })
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(req)));
    }

    LoginRequest reqExceeded = new LoginRequest("+918888800099", "Pwd1234567!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(request -> {
                  request.setRemoteAddr(clientIp);
                  return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqExceeded)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/rate-limited"))
        .andExpect(jsonPath("$.title").value("Rate Limited"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }
}
