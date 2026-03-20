package com.ebikes.workforce.support.web;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ebikes.workforce.constants.ApplicationConstants;
import com.ebikes.workforce.constants.MDCKeys;
import com.ebikes.workforce.dtos.responses.api.ErrorResponse;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.support.references.ReferenceGenerator;

public final class ErrorResponseBuilder {

  private ErrorResponseBuilder() {
    // prevent instantiaton
  }

  public static ErrorResponse buildErrorResponse(
      String detail, String requestPath, ResponseCode responseCode) {
    String errorReference = ReferenceGenerator.generateErrorReference();
    MDC.put(MDCKeys.ERROR_REFERENCE, errorReference);
    return ErrorResponse.from(detail, errorReference, requestPath, responseCode);
  }

  public static ErrorResponse buildErrorResponseWithErrors(
      String detail,
      List<ErrorResponse.ErrorDetail> errors,
      String requestPath,
      ResponseCode responseCode) {
    String errorReference = ReferenceGenerator.generateErrorReference();
    MDC.put(MDCKeys.ERROR_REFERENCE, errorReference);
    return ErrorResponse.withErrors(detail, errorReference, errors, requestPath, responseCode);
  }

  public static ResponseEntity<ErrorResponse> buildResponse(
      ErrorResponse errorResponse, HttpStatus status) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CONTENT_TYPE, ApplicationConstants.PROBLEM_JSON_MEDIA_TYPE)
        .body(errorResponse);
  }
}
