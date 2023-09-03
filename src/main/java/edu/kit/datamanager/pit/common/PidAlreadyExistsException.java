package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Indicates an internal error while updating a PID.
 */
public class PidAlreadyExistsException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;
    private static final HttpStatus HTTP_STATUS = HttpStatus.CONFLICT;

    public PidAlreadyExistsException(String pid) {
        super(HTTP_STATUS, String.format("The given PID already exists: %s", pid));
    }

}
