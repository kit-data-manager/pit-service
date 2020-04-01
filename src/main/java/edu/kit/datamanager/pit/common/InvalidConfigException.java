package edu.kit.datamanager.pit.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InvalidConfigException extends RuntimeException{

  public InvalidConfigException(String message){
    super(message);
  }

  /**
   *
   */
  private static final long serialVersionUID = 1L;

}
