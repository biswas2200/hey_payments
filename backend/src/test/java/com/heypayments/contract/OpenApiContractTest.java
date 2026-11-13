package com.heypayments.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTest extends BaseIntegrationTest {

  @Autowired private WebApplicationContext context;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("should diff implemented endpoints against docs/openapi.yaml contract")
  void shouldMatchOpenApiContractForPhase1() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    Path specPath = Paths.get("../docs/openapi.yaml").toAbsolutePath().normalize();
    if (!Files.exists(specPath)) {
      specPath = Paths.get("docs/openapi.yaml").toAbsolutePath().normalize();
    }
    assertThat(Files.exists(specPath)).as("docs/openapi.yaml must exist").isTrue();

    // 1. Load docs/openapi.yaml contract
    Map<String, Object> contractYaml;
    try (InputStream in = Files.newInputStream(specPath)) {
      contractYaml = new Yaml().load(in);
    }
    assertThat(contractYaml).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, Object> contractPaths = (Map<String, Object>) contractYaml.get("paths");
    assertThat(contractPaths).isNotNull();

    // 2. Fetch live /v3/api-docs from running application
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).isNotNull();

    JsonNode apiDocsNode = objectMapper.readTree(body);
    JsonNode implementedPathsNode = apiDocsNode.get("paths");
    assertThat(implementedPathsNode).isNotNull();

    // Phase 1 endpoints defined in docs/openapi.yaml
    String[] phase1ContractEndpoints = {
      "/auth/register", "/auth/login", "/auth/refresh", "/devices", "/devices/crl", "/keys/jwks"
    };

    for (String relPath : phase1ContractEndpoints) {
      String expectedImplementedPath = "/api/v1" + relPath;
      JsonNode pathNode = implementedPathsNode.get(expectedImplementedPath);
      assertThat(pathNode)
          .as("Implemented endpoint %s must be present in /v3/api-docs", expectedImplementedPath)
          .isNotNull();

      @SuppressWarnings("unchecked")
      Map<String, Object> contractMethods = (Map<String, Object>) contractPaths.get(relPath);
      for (String httpMethod : contractMethods.keySet()) {
        assertThat(pathNode.has(httpMethod.toLowerCase()))
            .as(
                "Method %s for %s must be present in /v3/api-docs",
                httpMethod, expectedImplementedPath)
            .isTrue();
      }
    }

    // Verify intentional additions documented in docs/proposals/002-openapi-phase1.md
    assertThat(implementedPathsNode.get("/api/v1/auth/verify-otp"))
        .as("Intentional addition /api/v1/auth/verify-otp must be implemented")
        .isNotNull();
    assertThat(implementedPathsNode.get("/api/v1/auth/logout"))
        .as("Intentional addition /api/v1/auth/logout must be implemented")
        .isNotNull();
  }
}
