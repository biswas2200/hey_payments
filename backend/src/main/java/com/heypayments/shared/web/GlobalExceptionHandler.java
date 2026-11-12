package com.heypayments.shared.web;

import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.domain.exception.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice translating domain exceptions and errors to RFC 9457 Problem Details. Enforces
 * security rule: stack traces and sensitive error details are never leaked.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Translates domain business exceptions into RFC 9457 Problem Details.
   *
   * @param ex the domain exception thrown by service or domain logic
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemResponse> handleDomainException(
      DomainException ex, HttpServletRequest request) {

    ErrorCode errorCode = ex.getErrorCode();
    String requestId = resolveRequestId();

    log.warn(
        "Domain exception [{}]: {} (requestId: {})", errorCode.name(), ex.getMessage(), requestId);

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            ex.getMessage(),
            request.getRequestURI(),
            requestId,
            ex.getFieldErrors());

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates validation failures into 400 VALIDATION_FAILED Problem Details.
   *
   * @param ex the method argument validation exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    BindingResult bindingResult = ex.getBindingResult();

    List<FieldErrorDetail> fieldErrors =
        bindingResult.getFieldErrors().stream()
            .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
            .toList();

    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
    log.warn(
        "Validation failed on {}: {} field error(s) (requestId: {})",
        request.getRequestURI(),
        fieldErrors.size(),
        requestId);

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "Validation failed for one or more fields.",
            request.getRequestURI(),
            requestId,
            fieldErrors);

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates unhandled unexpected exceptions into generic 500 INTERNAL Problem Details.
   *
   * @param ex the unhandled unexpected exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    log.error("Unhandled internal server error (requestId: {})", requestId, ex);

    ErrorCode errorCode = ErrorCode.INTERNAL;

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "An unexpected internal error occurred.",
            request.getRequestURI(),
            requestId,
            List.of());

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  private String resolveRequestId() {
    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
    return requestId != null ? requestId : "";
  }
}
