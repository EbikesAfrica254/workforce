package com.ebikes.workforce.exceptions;

import java.io.Serial;

import com.ebikes.workforce.enums.ResponseCode;

public class ResourceNotFoundException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
