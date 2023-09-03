package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Indicates an internal error while updating a PID.
 */
public class PidUpdateException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;
    private static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public PidUpdateException(String errorMessage) {
        super(HTTP_STATUS, String.format("Internal error while updating the PID: %s", errorMessage));
    }

}
