package com.heypayments.auth.infra.config;

import com.heypayments.auth.application.JwtTokenService;
import com.heypayments.crypto.application.ServerKeyService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

  @Bean
  public JwtTokenService jwtTokenService(ServerKeyService serverKeyService, Clock clock) {
    return new JwtTokenService(serverKeyService, clock);
  }
}
