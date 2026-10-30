package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which did not point to a profile or type definition.
 */
public class TypeNotFoundException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public TypeNotFoundException(String pid) {
		super(HTTP_STATUS, "The given PID \"" + pid + "\" is not a type in the configured registry.");
	}
}
