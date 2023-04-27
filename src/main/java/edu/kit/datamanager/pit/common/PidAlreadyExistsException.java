package edu.kit.datamanager.pit.common;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Indicates an internal error while updating a PID.
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class PidAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PidAlreadyExistsException(String pid) {
        super(String.format("The given PID already exists: %s", pid));
    }

}
