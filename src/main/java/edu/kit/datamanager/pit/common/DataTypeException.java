package edu.kit.datamanager.pit.common;

/**
 * Exception to use when the hidden linking between records in an identifier
 * system and a type registry are broken, e.g. because a PID record lists a
 * property identifier that is unregistered.
 * 
 */
public class DataTypeException extends Exception {

	public DataTypeException(String message) {
		super(message);
	}

}
