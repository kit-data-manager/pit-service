package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class PidNotFoundException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public PidNotFoundException(String pid) {
		super(HTTP_STATUS, "Identifier with value " + pid + " not found.");
	}

	public PidNotFoundException(String pid, Throwable e) {
		super(HTTP_STATUS, "Identifier with value " + pid + " not found.", e);
	}
}
