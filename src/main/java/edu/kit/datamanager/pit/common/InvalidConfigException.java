package edu.kit.datamanager.pit.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidConfigException extends ResponseStatusException {

  private static final long serialVersionUID = 1L;
  private static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

  public InvalidConfigException(String message) {
    super(HTTP_STATUS, message);
  }
}
