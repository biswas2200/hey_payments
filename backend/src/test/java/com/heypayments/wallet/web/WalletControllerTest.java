package com.heypayments.wallet.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.application.AuthService;
import com.heypayments.auth.application.OtpService;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.wallet.web.dto.FundRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class WalletControllerTest extends BaseIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private AuthService authService;
  @Autowired private OtpService otpService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DisplayName("GET /api/v1/wallets/me returns 401 when unauthenticated")
  void shouldReturn401WhenUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/wallets/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/wallets/me returns 200 with balances for authenticated payer")
  void shouldReturnWalletBalancesForPayer() throws Exception {
    String token = registerAndLogin("+919876500050", "PAYER", "Wallet User 1");

    mockMvc
        .perform(get("/api/v1/wallets/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mainBalance").value(0))
        .andExpect(jsonPath("$.offlineEscrow").value(0))
        .andExpect(jsonPath("$.activeBc").doesNotExist());
  }

  @Test
  @DisplayName(
      "POST /api/v1/wallets/me/fund funds simulated money and returns 201 with updated balance")
  void shouldFundWalletViaSimulator() throws Exception {
    String token = registerAndLogin("+919876500051", "PAYER", "Wallet User 2");
    String key = UUID.randomUUID().toString();

    FundRequest req = new FundRequest(250000L, "SIM_BANK");

    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mainBalance").value(250000))
        .andExpect(jsonPath("$.offlineEscrow").value(0));

    // Replaying with identical key returns cached 201 response with Idempotent-Replayed: true
    // header
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "true"))
        .andExpect(jsonPath("$.mainBalance").value(250000));

    // Replaying with same key and different payload returns 422 IDEMPOTENCY_KEY_REUSED
    FundRequest differentReq = new FundRequest(100000L, "SIM_BANK");
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(differentReq)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.type").value("https://heypayments.dev/errors/idempotency-key-reused"));
  }

  @Test
  @DisplayName("HIGH-1: GET and POST /api/v1/wallets/me reject non-PAYER roles with 403 Forbidden")
  void shouldRejectNonPayerRolesWithForbidden() throws Exception {
    String merchantToken = registerAndLogin("+919876500053", "MERCHANT", "Merchant User 1");

    mockMvc
        .perform(get("/api/v1/wallets/me").header("Authorization", "Bearer " + merchantToken))
        .andExpect(status().isForbidden());

    FundRequest req = new FundRequest(10000L, "SIM_BANK");
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + merchantToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
      "HIGH-3: POST /api/v1/wallets/me/fund rejects missing or invalid UUID Idempotency-Key with 400 Bad Request")
  void shouldRejectMissingOrInvalidIdempotencyKey() throws Exception {
    String token = registerAndLogin("+919876500054", "PAYER", "Wallet User 4");
    FundRequest req = new FundRequest(5000L, "SIM_BANK");

    // Missing header
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());

    // Invalid non-UUID format
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "not-a-valid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
      "MEDIUM-1: POST /api/v1/wallets/me/fund rejects amount exceeding ₹50,000 (5,000,000 paise) with 400 Bad Request")
  void shouldRejectFundingWhenAmountExceedsMaxCap() throws Exception {
    String token = registerAndLogin("+919876500055", "PAYER", "Wallet User 5");

    // Attempt to fund 50,001 INR (5,000,100 paise) -> rejected by @Max validator
    FundRequest req = new FundRequest(5_000_100L, "SIM_BANK");

    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
      "POST /api/v1/wallets/me/fund returns 422 LIMIT_EXCEEDED when cumulative daily cap of ₹50,000 is exceeded")
  void shouldRejectFundingWhenDailyLimitExceeded() throws Exception {
    String token = registerAndLogin("+919876500052", "PAYER", "Wallet User 3");

    // First fund 40,000 INR (4,000,000 paise)
    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FundRequest(4_000_000L, "SIM_BANK"))))
        .andExpect(status().isCreated());

    // Second fund 20,000 INR (2,000,000 paise) -> cumulative 60,000 > 50,000
    FundRequest req = new FundRequest(2_000_000L, "SIM_BANK");

    mockMvc
        .perform(
            post("/api/v1/wallets/me/fund")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value("https://heypayments.dev/errors/limit-exceeded"));
  }

  private String registerAndLogin(String phone, String role, String displayName) {
    authService.register(
        new RegisterRequest(
            phone, "user" + UUID.randomUUID() + "@example.com", "Password123!", role, displayName));
    String otp = otpService.getRecentOtpForPhone(phone);
    authService.verifyOtp(phone, otp);
    var loginResult = authService.login(new LoginRequest(phone, "Password123!"));
    return loginResult.tokenResponse().accessToken();
  }
}
