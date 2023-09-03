package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;

/**
 * This validation strategy will **not** do any validation.
 */
public class NoValidationStrategy implements IValidationStrategy {

    @Override
    public void validate(PIDRecord pidRecord) throws RecordValidationException, ExternalServiceException {
        // No validation will take place.
    }
    
}
