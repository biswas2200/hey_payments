package com.heypayments.shared.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.heypayments.auth.infra.config.SecurityConfig;
import com.heypayments.auth.infra.security.JwtAuthenticationFilter;
import com.heypayments.auth.infra.security.RateLimitingFilter;
import com.heypayments.shared.infra.security.ProblemAccessDeniedHandler;
import com.heypayments.shared.infra.security.ProblemAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({
  SecurityConfig.class,
  ProblemAuthenticationEntryPoint.class,
  ProblemAccessDeniedHandler.class
})
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockitoBean private RateLimitingFilter rateLimitingFilter;

  @BeforeEach
  void setUp() throws Exception {
    doAnswer(
            invocation -> {
              FilterChain chain = invocation.getArgument(2);
              chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(jwtAuthenticationFilter)
        .doFilter(any(), any(), any());

    doAnswer(
            invocation -> {
              FilterChain chain = invocation.getArgument(2);
              chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(rateLimitingFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  @DisplayName("GET /api/health should return UP status without authentication")
  void shouldReturnHealthUp() throws Exception {
    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("Hey Payments Backend"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }
}
