package com.heypayments.shared.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies production profile property resolution per docs/reviews/REVIEW-phase-0.md Condition C.
 */
class ProductionProfileConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withPropertyValues("spring.profiles.active=prod")
          .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class));

  @Test
  @DisplayName("prod profile should fail fast when SPRING_DATA_REDIS_PASSWORD is not provided")
  void prodProfileShouldFailWithoutRedisPassword() {
    contextRunner
        .withPropertyValues(
            "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/test",
            "DB_USER=test",
            "DB_PASSWORD=secret",
            "SPRING_DATA_REDIS_HOST=localhost",
            "SPRING_DATA_REDIS_PORT=6379")
        .run(
            context -> {
              org.assertj.core.api.Assertions.assertThatThrownBy(
                      () ->
                          context
                              .getEnvironment()
                              .getRequiredProperty("spring.data.redis.password"))
                  .isInstanceOf(RuntimeException.class)
                  .hasMessageContaining(
                      "Could not resolve placeholder 'SPRING_DATA_REDIS_PASSWORD'");
            });
  }

  @Test
  @DisplayName(
      "prod profile should configure RedisProperties when SPRING_DATA_REDIS_PASSWORD is provided")
  void prodProfileShouldSucceedWithRedisPassword() {
    contextRunner
        .withPropertyValues(
            "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/test",
            "DB_USER=test",
            "DB_PASSWORD=secret",
            "SPRING_DATA_REDIS_HOST=localhost",
            "SPRING_DATA_REDIS_PORT=6379",
            "SPRING_DATA_REDIS_PASSWORD=prod-secret-password")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              DataRedisProperties redisProps = context.getBean(DataRedisProperties.class);
              assertThat(redisProps.getPassword()).isEqualTo("prod-secret-password");
            });
  }
}
