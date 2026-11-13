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

    // Implemented endpoints defined in docs/openapi.yaml
    String[] implementedEndpoints = {
      "/auth/register",
      "/auth/login",
      "/auth/verify-otp",
      "/auth/refresh",
      "/auth/logout",
      "/auth/pin",
      "/devices",
      "/devices/me",
      "/devices/crl",
      "/keys/jwks",
      "/wallets/me",
      "/wallets/me/fund"
    };

    for (String relPath : implementedEndpoints) {
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
  }

  @Test
  @DisplayName("should match request and response schemas of implemented endpoints")
  void shouldMatchRequestAndResponseSchemas() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    Path specPath = Paths.get("../docs/openapi.yaml").toAbsolutePath().normalize();
    if (!Files.exists(specPath)) {
      specPath = Paths.get("docs/openapi.yaml").toAbsolutePath().normalize();
    }
    assertThat(Files.exists(specPath)).as("docs/openapi.yaml must exist").isTrue();

    Map<String, Object> contractYaml;
    try (InputStream in = Files.newInputStream(specPath)) {
      contractYaml = new Yaml().load(in);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> contractSchemas =
        (Map<String, Object>) ((Map<String, Object>) contractYaml.get("components")).get("schemas");

    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode apiDocsNode = objectMapper.readTree(body);
    JsonNode implementedSchemas = apiDocsNode.get("components").get("schemas");
    assertThat(implementedSchemas).isNotNull();

    // Verify key request/response schemas exist and property sets match
    String[] targetSchemas = {
      "RegisterRequest",
      "LoginRequest",
      "VerifyOtpRequest",
      "SetPinRequest",
      "IdResponse",
      "TokenResponse",
      "RegisterDeviceRequest",
      "DeviceResponse",
      "WalletResponse",
      "FundRequest"
    };

    for (String schemaName : targetSchemas) {
      assertThat(implementedSchemas.has(schemaName))
          .as("Schema %s must exist in Springdoc /v3/api-docs", schemaName)
          .isTrue();

      @SuppressWarnings("unchecked")
      Map<String, Object> expectedSchema = (Map<String, Object>) contractSchemas.get(schemaName);
      @SuppressWarnings("unchecked")
      Map<String, Object> expectedProps = (Map<String, Object>) expectedSchema.get("properties");

      JsonNode liveSchemaNode = implementedSchemas.get(schemaName);
      JsonNode liveProps = liveSchemaNode.get("properties");
      assertThat(liveProps).as("Properties node must exist for schema %s", schemaName).isNotNull();

      for (String propName : expectedProps.keySet()) {
        assertThat(liveProps.has(propName))
            .as("Property '%s' in schema '%s' must match /v3/api-docs", propName, schemaName)
            .isTrue();
      }
    }
  }
}
