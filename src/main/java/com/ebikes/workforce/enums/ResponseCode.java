package com.ebikes.workforce.enums;

import static com.ebikes.workforce.constants.EventConstants.EventSource.HOST;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {
  AUTHENTICATION_FAILED(
      "AUTHENTICATION_FAILED",
      "Authentication failed. Please check your credentials and try again.",
      HttpStatus.UNAUTHORIZED),
  DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Duplicate resource detected.", HttpStatus.CONFLICT),
  EXTERNAL_ERROR(
      "EXTERNAL_ERROR",
      "An error occurred while communicating with an external service.",
      HttpStatus.INTERNAL_SERVER_ERROR),
  FORBIDDEN(
      "FORBIDDEN", "You do not have permission to perform this operation.", HttpStatus.FORBIDDEN),
  GATEWAY_TIMEOUT(
      "GATEWAY_TIMEOUT",
      "The external service did not respond in time.",
      HttpStatus.GATEWAY_TIMEOUT),
  INSUFFICIENT_SCOPE(
      "INSUFFICIENT_SCOPE",
      "The token does not have the required scope to perform this operation.",
      HttpStatus.FORBIDDEN),
  INTERNAL_SERVER_ERROR(
      "INTERNAL_SERVER_ERROR",
      "An unexpected error occurred while processing your request. Please try again or contact"
          + " support.",
      HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_ARGUMENTS(
      "INVALID_ARGUMENTS",
      "Request contains invalid or incomplete arguments.",
      HttpStatus.BAD_REQUEST),
  INVALID_FORMAT("INVALID_FORMAT", "Field format is invalid.", HttpStatus.BAD_REQUEST),
  INVALID_STATE(
      "INVALID_STATE",
      "The resource is in an invalid state for the requested operation.",
      HttpStatus.BAD_REQUEST),
  MISSING_REQUIRED_FIELD(
      "MISSING_REQUIRED_FIELD",
      "Required field is missing in the request.",
      HttpStatus.BAD_REQUEST),
  RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", HttpStatus.TOO_MANY_REQUESTS),
  RESOURCE_NOT_FOUND(
      "RESOURCE_NOT_FOUND", "The specified resource does not exist.", HttpStatus.NOT_FOUND),

  // Service-scoped — prefixed with service identifier
  INVALID_SECURITY_CODE(
      HOST + ".INVALID_SECURITY_CODE",
      "The provided code is invalid or malformed.",
      HttpStatus.BAD_REQUEST),
  MAX_RETRIES_EXCEEDED(
      HOST + ".MAX_RETRIES_EXCEEDED",
      "The maximum number of attempts has been exceeded.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_ALREADY_USED(
      HOST + ".SECURITY_CODE_ALREADY_USED",
      "This code has already been used.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_EXPIRED(
      HOST + ".SECURITY_CODE_EXPIRED",
      "The code has expired. Please request a new one.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_TYPE_MISMATCH(
      HOST + ".SECURITY_CODE_TYPE_MISMATCH",
      "The code type does not match the expected type.",
      HttpStatus.BAD_REQUEST);

  private final String code;
  private final String userMessage;
  private final HttpStatus httpStatus;

}
