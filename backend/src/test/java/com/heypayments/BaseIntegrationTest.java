package com.heypayments;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base integration test managing a singleton PostgreSQL Testcontainer across all test classes.
 * Conforms to docs/reviews/REVIEW-phase-0.md MUST-FIX A.1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @ServiceConnection protected static final PostgreSQLContainer<?> postgres;

  static {
    postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("heypayments")
            .withUsername("hp")
            .withPassword("test");
    postgres.start();
  }
}
