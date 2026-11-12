package com.heypayments.device.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.application.AuthService;
import com.heypayments.auth.application.OtpService;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.auth.web.dto.VerifyOtpRequest;
import com.heypayments.crypto.application.ServerKeyService;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.SignedEnvelope;
import com.heypayments.device.web.dto.RegisterDeviceRequest;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class DeviceControllerTest extends BaseIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private AuthService authService;
  @Autowired private OtpService otpService;
  @Autowired private CryptoService cryptoService;
  @Autowired private ServerKeyService serverKeyService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DisplayName("Payer device registration: registers active device and prevents duplicate active devices")
  void shouldRegisterPayerDevice() throws Exception {
    String token = registerAndLogin("+919876500010", "PAYER", "Payer One");

    KeyPair clientKeyPair = cryptoService.generateP256KeyPair();
    Map<String, String> jwk =
        cryptoService.toJwk((ECPublicKey) clientKeyPair.getPublic(), "client-key-1");

    RegisterDeviceRequest req = new RegisterDeviceRequest(jwk, "Pixel 8", "Android/Chrome");

    // 1. Register device
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/devices")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.deviceId").exists())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.lastCounter").value(0))
            .andExpect(jsonPath("$.merchantCertificate").doesNotExist())
            .andReturn();

    String deviceId =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("deviceId").asText();

    // 2. Second registration attempt for same user/kind must return 409 DEVICE_ALREADY_ACTIVE
    mockMvc
        .perform(
            post("/api/v1/devices")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DEVICE_ALREADY_ACTIVE"));

    // 3. GET /devices/me
    mockMvc
        .perform(get("/api/v1/devices/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deviceId").value(deviceId))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    // 4. Close device
    mockMvc
        .perform(
            delete("/api/v1/devices/" + deviceId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    // 5. Check CRL includes closed device
    mockMvc
        .perform(get("/api/v1/devices/crl").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revoked").isArray())
        .andExpect(jsonPath("$.revoked[?(@.deviceId == '" + deviceId + "')].status").value("CLOSED"));
  }

  @Test
  @DisplayName("Merchant device registration: issues verifiable Merchant Certificate (MC)")
  void shouldRegisterMerchantDeviceAndIssueCertificate() throws Exception {
    String token = registerAndLogin("+919876500011", "MERCHANT", "Ravi Kirana Store");

    KeyPair clientKeyPair = cryptoService.generateP256KeyPair();
    Map<String, String> jwk =
        cryptoService.toJwk((ECPublicKey) clientKeyPair.getPublic(), "merchant-key-1");

    RegisterDeviceRequest req =
        new RegisterDeviceRequest(jwk, "POS Terminal A", "Linux/Nginx Terminal");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/devices")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.deviceId").exists())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.merchantCertificate").exists())
            .andExpect(jsonPath("$.merchantCertificate.p.t").value("MC"))
            .andExpect(jsonPath("$.merchantCertificate.p.displayName").value("Ravi Kirana Store"))
            .andExpect(jsonPath("$.merchantCertificate.s").exists())
            .andReturn();

    // Verify MC envelope with server public key
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode mcNode = root.get("merchantCertificate");

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = objectMapper.convertValue(mcNode.get("p"), Map.class);
    String signature = mcNode.get("s").asText();

    SignedEnvelope mcEnvelope = new SignedEnvelope(payload, signature);
    ECPublicKey serverPub = serverKeyService.getActivePublicKey();

    assertThat(serverKeyService.verifyEnvelope(mcEnvelope, serverPub)).isTrue();
  }

  private String registerAndLogin(String phone, String role, String displayName) {
    authService.register(
        new RegisterRequest(phone, "user@example.com", "Password123!", role, displayName));
    String otp = otpService.getRecentOtpForPhone(phone);
    authService.verifyOtp(phone, otp);
    var loginResult = authService.login(new LoginRequest(phone, "Password123!"));
    return loginResult.tokenResponse().accessToken();
  }
}
