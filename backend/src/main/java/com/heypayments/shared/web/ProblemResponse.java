package com.heypayments.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.heypayments.shared.domain.exception.FieldErrorDetail;
import java.util.List;

/**
 * Standard RFC 9457 Problem Details payload structure.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String requestId,
        List<FieldErrorDetail> errors
) {
}
