package com.heypayments.auth.infra.security;

import com.heypayments.auth.application.JwtClaims;
import com.heypayments.auth.application.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenService jwtTokenService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length()).trim();
      try {
        JwtClaims claims = jwtTokenService.validateAndParseToken(token);

        List<SimpleGrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(claims.userId(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (Exception e) {
        log.debug(
            "JWT validation failed for request {}: {}", request.getRequestURI(), e.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }
}
