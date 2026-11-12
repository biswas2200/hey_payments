package com.heypayments.crypto.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.heypayments.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class JwksControllerTest extends BaseIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Test
  @DisplayName("GET /api/v1/keys/jwks should be public and return valid JWKS schema")
  void shouldReturnJwksPublicly() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    mockMvc
        .perform(get("/api/v1/keys/jwks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys").isArray())
        .andExpect(jsonPath("$.keys[0].kid").exists())
        .andExpect(jsonPath("$.keys[0].kty").value("EC"))
        .andExpect(jsonPath("$.keys[0].crv").value("P-256"))
        .andExpect(jsonPath("$.keys[0].x").exists())
        .andExpect(jsonPath("$.keys[0].y").exists())
        .andExpect(jsonPath("$.keys[0].status").exists());
  }
}
