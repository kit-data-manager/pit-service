package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class PidNotFoundException extends ResponseStatusException {

	@Serial
	private static final long serialVersionUID = 3362829471655054621L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;
	public static final String ID_NOT_FOUND_MSG = "Identifier with value %s not found.";
	public static final String REASON_MSG = "%s Reason: %s";

	public PidNotFoundException(String pid) {
		super(HTTP_STATUS, ID_NOT_FOUND_MSG.formatted(pid));
	}

	public PidNotFoundException(String pid, String reason) {
		super(HTTP_STATUS, REASON_MSG.formatted(ID_NOT_FOUND_MSG.formatted(pid), reason));
	}

	public PidNotFoundException(String pid, String reason, Throwable e) {
		super(HTTP_STATUS, REASON_MSG.formatted(ID_NOT_FOUND_MSG.formatted(pid), reason), e);
	}

	public PidNotFoundException(String pid, Throwable e) {
		super(HTTP_STATUS, ID_NOT_FOUND_MSG.formatted(pid), e);
	}
}
