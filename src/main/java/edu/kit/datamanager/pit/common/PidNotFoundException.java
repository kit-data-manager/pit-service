package edu.kit.datamanager.pit.common;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PidNotFoundException extends IOException {

	private static final long serialVersionUID = 1L;

	public PidNotFoundException(String pid) {
		super("Identifier with value " + pid + " not found.");
	}

}
