package edu.kit.datamanager.pit;

/**
 * Exception to use when the hidden linking between records in an identifier
 * system and a type registry are broken, e.g. because a PID record lists a
 * property identifier that is unregistered.
 * 
 */
public class InconsistentRecordsException extends Exception {

	public InconsistentRecordsException(String message) {
		super(message);
	}

}
