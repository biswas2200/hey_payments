package com.heypayments.shared.infra.config;

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

/** Base Spring Security filter chain configuration with method security enabled. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final Environment environment;
  private final ProblemAuthenticationEntryPoint authenticationEntryPoint;
  private final ProblemAccessDeniedHandler accessDeniedHandler;

  /**
   * Constructs the security configuration with custom authentication and access denial handlers.
   *
   * @param environment current active environment profiles
   * @param authenticationEntryPoint custom RFC 9457 authentication entry point
   * @param accessDeniedHandler custom RFC 9457 access denied handler
   */
  public SecurityConfig(
      Environment environment,
      ProblemAuthenticationEntryPoint authenticationEntryPoint,
      ProblemAccessDeniedHandler accessDeniedHandler) {
    this.environment = environment;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  /**
   * Configures stateless HTTP security with public endpoints for health and documentation.
   *
   * @param http the HttpSecurity builder
   * @return configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
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
                      "/api/health", "/actuator/health", "/actuator/health/**", "/actuator/info")
                  .permitAll();
              if (isDev) {
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll();
              }
              auth.anyRequest().authenticated();
            });

    return http.build();
  }
}
