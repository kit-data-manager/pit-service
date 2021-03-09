package edu.kit.datamanager.pit.common;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which did not point to a profile or type definition.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TypeNotFoundException extends IOException {

	private static final long serialVersionUID = 1L;

	public TypeNotFoundException(String pid) {
		super("The given PID " + pid + " is not a type in the configured registry.");
	}
}
