package com.heypayments.shared.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

class ProblemSecurityHandlersTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ProblemAuthenticationEntryPoint entryPoint =
      new ProblemAuthenticationEntryPoint(
          new StaticListableBeanFactory().getBeanProvider(JsonMapper.class));
  private final ProblemAccessDeniedHandler deniedHandler =
      new ProblemAccessDeniedHandler(
          new StaticListableBeanFactory().getBeanProvider(JsonMapper.class));

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("entry point should resolve requestId from MDC when present")
  void entryPointShouldUseMdcRequestId() throws IOException {
    MDC.put("requestId", "mdc-req-123");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("unauth"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("mdc-req-123");
    assertThat(response.getContentAsString()).contains("UNAUTHENTICATED");
  }

  @Test
  @DisplayName("entry point should resolve requestId from header when MDC is empty")
  void entryPointShouldUseHeaderRequestId() throws IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
    request.addHeader("X-Request-Id", "hdr-req-456");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("unauth"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("hdr-req-456");
  }

  @Test
  @DisplayName("entry point should generate UUID when MDC and header are missing")
  void entryPointShouldGenerateUuidWhenMissing() throws IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("unauth"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString())
        .contains("https://heypayments.dev/errors/unauthenticated");
  }

  @Test
  @DisplayName("access denied handler should resolve requestId and return 403 problem details")
  void accessDeniedHandlerShouldReturnForbidden() throws IOException {
    MDC.put("requestId", "mdc-denied-123");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    deniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("FORBIDDEN");
    assertThat(response.getContentAsString()).contains("mdc-denied-123");
  }

  @Test
  @DisplayName("access denied handler should resolve requestId from header and fallback to UUID")
  void accessDeniedHandlerFallbackRequestId() throws IOException {
    MockHttpServletRequest requestWithHdr = new MockHttpServletRequest("POST", "/api/v1/test");
    requestWithHdr.addHeader("X-Request-Id", "hdr-denied-456");
    MockHttpServletResponse respWithHdr = new MockHttpServletResponse();

    deniedHandler.handle(requestWithHdr, respWithHdr, new AccessDeniedException("forbidden"));
    assertThat(respWithHdr.getContentAsString()).contains("hdr-denied-456");

    MockHttpServletRequest emptyReq = new MockHttpServletRequest("POST", "/api/v1/test");
    MockHttpServletResponse emptyResp = new MockHttpServletResponse();
    deniedHandler.handle(emptyReq, emptyResp, new AccessDeniedException("forbidden"));
    assertThat(emptyResp.getStatus()).isEqualTo(403);
    assertThat(emptyResp.getContentAsString()).contains("https://heypayments.dev/errors/forbidden");
  }
}
