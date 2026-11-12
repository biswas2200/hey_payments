package com.heypayments.device.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.heypayments.crypto.domain.SignedEnvelope;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceResponse(
    UUID deviceId, String status, long lastCounter, SignedEnvelope merchantCertificate) {}
