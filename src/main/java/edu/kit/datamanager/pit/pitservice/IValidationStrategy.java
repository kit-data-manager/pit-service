package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;

public interface IValidationStrategy {
    /**
     * Validate the given record. Exceptions indicate failure.
     * 
     * The record does not have to be registered.
     * 
     * @param pidRecord the PID record to validate.
     */
    void validate(PIDRecord pidRecord) throws RecordValidationException, ExternalServiceException;
}
