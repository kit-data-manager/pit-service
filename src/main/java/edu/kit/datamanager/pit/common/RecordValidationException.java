package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class RecordValidationException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public RecordValidationException(String pid) {
		super(HTTP_STATUS, "Validation of record " + pid + " failed.");
	}

	public RecordValidationException(String pid, String reason) {
		super(HTTP_STATUS, "Validation of record " + pid + " failed. Reason:\n" + reason);
	}

}
