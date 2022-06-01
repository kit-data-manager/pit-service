package edu.kit.datamanager.pit.common;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Indicates an internal error while updating a PID.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class PidUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PidUpdateException(String error_message) {
        super(String.format("Internal error while updating the PID: %s", error_message));
    }

}
