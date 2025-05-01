package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import edu.kit.datamanager.pit.domain.PIDRecord;

import java.io.Serial;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class RecordValidationException extends ResponseStatusException {

	private static final String VALIDATION_OF_RECORD = "Validation of record ";
	@Serial
	private static final long serialVersionUID = -7287999233733933282L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	// For cases in which the PID record should be appended to the error response.
	private final PIDRecord pidRecord;

	public RecordValidationException(PIDRecord pidRecord) {
		super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed.");
		this.pidRecord = pidRecord;
	}

	public RecordValidationException(PIDRecord pidRecord, String reason) {
		super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed. Reason:\n" + reason);
		this.pidRecord = pidRecord;
	}

	public PIDRecord getPidRecord() {
		return pidRecord;
	}
}
