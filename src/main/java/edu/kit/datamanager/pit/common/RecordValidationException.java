package edu.kit.datamanager.pit.common;

import org.springframework.web.server.ResponseStatusException;

import edu.kit.datamanager.pit.domain.PIDRecord;

import java.util.Optional;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class RecordValidationException extends ResponseStatusException {

	private static final String VALIDATION_OF_RECORD = "Validation of record ";
	private static final long serialVersionUID = 1L;
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	// For cases in which the PID record shold be appended to the error response.
	private final transient Optional<PIDRecord> pidRecord;

	public RecordValidationException(PIDRecord pidRecord) {
		super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed.");
		this.pidRecord = Optional.of(pidRecord);
	}

	public RecordValidationException(PIDRecord pidRecord, String reason) {
		super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed. Reason:\n" + reason);
		this.pidRecord = Optional.of(pidRecord);
	}

	public Optional<PIDRecord> getPidRecord() {
		return pidRecord;
	}
}
