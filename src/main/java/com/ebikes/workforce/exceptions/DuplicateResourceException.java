package com.ebikes.workforce.exceptions;

import java.io.Serial;

import com.ebikes.workforce.enums.ResponseCode;

public class DuplicateResourceException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateResourceException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
