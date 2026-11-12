package com.heypayments.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("should propagate existing X-Request-Id header to response and MDC")
    void shouldPropagateExistingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo("req-12345");
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-12345");
        assertThat(MDC.get("requestId")).isNull(); // Cleaned up in finally
    }

    @Test
    @DisplayName("should generate UUIDv7 when X-Request-Id header is missing")
    void shouldGenerateRequestIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isNotBlank();
        });

        String generatedId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(generatedId).isNotBlank();
        assertThat(MDC.get("requestId")).isNull();
    }
}
