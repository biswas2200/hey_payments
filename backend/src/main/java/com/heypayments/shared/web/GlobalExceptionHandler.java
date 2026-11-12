package com.heypayments.shared.web;

import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.domain.exception.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler providing RFC 9457 Problem Details responses for all domain, validation,
 * and standard web protocol exceptions.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Translates domain exceptions into RFC 9457 Problem Details.
   *
   * @param ex the domain exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemResponse> handleDomainException(
      DomainException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ex.getErrorCode();

    log.warn(
        "Domain error [{}] on {} (requestId: {}): {}",
        errorCode.name(),
        request.getRequestURI(),
        requestId,
        ex.getMessage());

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
   * Translates malformed JSON request bodies into 400 VALIDATION_FAILED Problem Details.
   *
   * @param ex the HTTP message not readable exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
    log.warn("Malformed request payload on {} (requestId: {})", request.getRequestURI(), requestId);

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "Malformed request body or invalid JSON syntax.",
            request.getRequestURI(),
            requestId,
            List.of());

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates URI parameter type mismatches into 400 VALIDATION_FAILED Problem Details.
   *
   * @param ex the method argument type mismatch exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "Invalid parameter format: " + ex.getName(),
            request.getRequestURI(),
            requestId,
            List.of(new FieldErrorDetail(ex.getName(), "Invalid format")));

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates missing static or API route resources into 404 NOT_FOUND Problem Details.
   *
   * @param ex the no resource found exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemResponse> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ErrorCode.NOT_FOUND;

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "The requested resource was not found: " + request.getRequestURI(),
            request.getRequestURI(),
            requestId,
            List.of());

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates unsupported HTTP methods into 405 METHOD_NOT_ALLOWED Problem Details.
   *
   * @param ex the HTTP request method not supported exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "HTTP method " + ex.getMethod() + " is not supported for this endpoint.",
            request.getRequestURI(),
            requestId,
            List.of());

    return ResponseEntity.status(errorCode.getHttpStatus())
        .header("Content-Type", "application/problem+json")
        .body(body);
  }

  /**
   * Translates security access denied exceptions into 403 FORBIDDEN Problem Details.
   *
   * @param ex the access denied exception
   * @param request the current HTTP servlet request
   * @return HTTP problem details response
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {

    String requestId = resolveRequestId();
    ErrorCode errorCode = ErrorCode.FORBIDDEN;

    ProblemResponse body =
        new ProblemResponse(
            errorCode.getTypeUri(),
            errorCode.getTitle(),
            errorCode.getStatusCode(),
            errorCode.name(),
            "Access is denied.",
            request.getRequestURI(),
            requestId,
            List.of());

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
