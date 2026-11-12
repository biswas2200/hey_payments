package com.heypayments.auth.infra.config;

import com.heypayments.auth.infra.security.JwtAuthenticationFilter;
import com.heypayments.auth.infra.security.RateLimitingFilter;
import com.heypayments.shared.infra.security.ProblemAccessDeniedHandler;
import com.heypayments.shared.infra.security.ProblemAuthenticationEntryPoint;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Base Spring Security filter chain configuration with method security enabled. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final Environment environment;
  private final ProblemAuthenticationEntryPoint authenticationEntryPoint;
  private final ProblemAccessDeniedHandler accessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitingFilter rateLimitingFilter;

  public SecurityConfig(
      Environment environment,
      ProblemAuthenticationEntryPoint authenticationEntryPoint,
      ProblemAccessDeniedHandler accessDeniedHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitingFilter rateLimitingFilter) {
    this.environment = environment;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitingFilter = rateLimitingFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> {})
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(
                      "/api/health",
                      "/actuator/health",
                      "/actuator/health/**",
                      "/actuator/info",
                      "/api/v1/keys/jwks",
                      "/api/v1/auth/register",
                      "/api/v1/auth/verify-otp",
                      "/api/v1/auth/login",
                      "/api/v1/auth/refresh",
                      "/api/v1/auth/logout")
                  .permitAll();
              if (isDev) {
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll();
              }
              auth.anyRequest().authenticated();
            })
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
