package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

/**
 * Core interface for clients to contact. Implementations will provide the
 * necessary fixation in a concrete protocol (e.g. HTTP-REST).
 *
 */
public interface ITypingService extends IIdentifierSystem {

    public void setValidationStrategy(IValidationStrategy strategy);

    public void validate(PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException;

    /**
     * Returns an operations instance, configured with this typingService.
     * <p>
     * Convenience method for `new Operations(typingService)`.
     * 
     * @return an operation instance.
     */
    public Operations getOperations();
}
