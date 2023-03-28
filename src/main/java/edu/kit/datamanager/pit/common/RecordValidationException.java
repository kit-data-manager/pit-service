package edu.kit.datamanager.pit.common;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class RecordValidationException extends IOException {

	private static final long serialVersionUID = 1L;

	public RecordValidationException(String pid) {
		super("Validation of record " + pid + " failed.");
	}

	public RecordValidationException(String pid, String reason) {
		super("Validation of record " + pid + " failed. Reason:\n" + reason);
	}

}
