package edu.kit.datamanager.pit.pidsystem;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffix;

import java.util.Collection;
import java.util.Optional;

/**
 * Main abstraction interface towards the identifier system containing
 * registered identifiers and associated state information.
 *
 */
public interface IIdentifierSystem {

    /**
     * Returns the configured prefix of this PID system.
     * 
     * If this system can create PIDs, the prefix is the one it uses to create PIDs.
     * Otherwise, it does not return a prefix.
     * 
     * @return the prefix this system uses to create PIDs, if it can create PIDs,
     *         empty otherwise.
     */
    public Optional<String> getPrefix();

    /**
     * Appends the given PID to the prefix, if possible.
     * 
     * It may not be possible if no prefix is present, or if the PID already starts
     * with a the prefix. The returnes String is then exaxtly the same.
     * 
     * @param pid the PID to append to the prefix.
     * @return the PID with the prefix appended, if possible.
     * @throws InvalidConfigException if the system can n.
     */
    public default String appendPrefixIfAbsent(String pid) throws InvalidConfigException {
        Optional<String> prefix = this.getPrefix();
        if (prefix.isPresent() && !pid.startsWith(prefix.get())) {
            return new PidSuffix(pid).getWithPrefix(prefix.get());
        } else {
            return pid;
        }
    }

    /**
     * Checks whether the given PID is already registered.
     *
     * @param pid the PID to check.
     * @return true, if the PID is registered, false otherwise.
     * @throws ExternalServiceException on commonication errors or errors on other
     *         services.
     */
    public boolean isPidRegistered(String pid) throws ExternalServiceException;

    /**
     * Checks whether the given PID is already registered.
     * 
     * Assumes the PID to be the configured prefix of the system combined with the
     * given suffix.
     * 
     * @param suffix the given suffix, which, appended to the configured prefix,
     *        forms the PID to check.
     * @return true, if the PID is registered, false otherwise.
     * @throws ExternalServiceException on commonication errors or errors on other
     *         services.
     * @throws InvalidConfigException if there is no prefix configured to append to
     *         the suffix.
     */
    public default boolean isPidRegistered(PidSuffix suffix) throws ExternalServiceException, InvalidConfigException {
        String prefix = getPrefix().orElseThrow(() -> new InvalidConfigException("This system cannot create PIDs."));
        return isPidRegistered(suffix.getWithPrefix(prefix));
    }

    /**
     * Queries all properties from the given PID, independent of types.
     *
     * @param pid the PID to query the properties from.
     * @return a PID information record with its PID and attribute-value-pairs. The
     *         property names will be empty strings. Contains all property values
     *         present in the record of the given PID.
     * @throws PidNotFoundException if the pid is not registered.
     * @throws ExternalServiceException on commonication errors or errors on other
     *         services.
     */
    public PIDRecord queryPid(String pid) throws PidNotFoundException, ExternalServiceException;

    /**
     * Registers a new PID with given property values. The method takes the PID from
     * the record and treats it as a suffix.
     * 
     * The method must process the given PID using the
     * {@link #registerPid(PIDRecord)} method.
     *
     * @param pidRecord contains the initial PID record.
     * @return the PID that was assigned to the record.
     * @throws PidAlreadyExistsException if the PID already exists
     * @throws ExternalServiceException if an error occured in communication with
     *         other services.
     * @throws RecordValidationException if record validation errors occurred.
     */
    public default String registerPid(final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException, RecordValidationException {
        if (pidRecord.getPid() == null) {
            throw new RecordValidationException(pidRecord, "PID must not be null.");
        }
        if (pidRecord.getPid().isEmpty()) {
            throw new RecordValidationException(pidRecord, "PID must not be empty.");
        }
        pidRecord.setPid(
            appendPrefixIfAbsent(pidRecord.getPid())
        );
        return registerPidUnchecked(pidRecord);
    }

    /**
     * Registers the given record with its given PID, without applying any checks.
     * Recommended to use {@link #registerPid(PIDRecord)} instead.
     * 
     * As an implementor, you can assume the PID to be not null, valid,
     * non-registered, and prefixed.
     * 
     * @param pidRecord the record to register.
     * @return the PID that was assigned to the record.
     * @throws PidAlreadyExistsException if the PID already exists
     * @throws ExternalServiceException if an error occured in communication with
     *         other services.
     */
    public String registerPidUnchecked(final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException;

    /**
     * Updates an existing record with the new given values. If the PID in the given
     * record is not valid, it will return false.
     * 
     * @param pidRecord Assumes an existing, valid PID inside this record.
     * @return false if there was no existing, valid PID in this record.
     * @throws PidNotFoundException if PID is not registered.
     * @throws ExternalServiceException if an error occured in communication with
     *         other services.
     * @throws RecordValidationException if record validation errors occurred.
     */
    public boolean updatePid(PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException;

    /**
     * Remove the given PID.
     * 
     * Obviously, this method is only for testing purposes, since we should not
     * delete persistent identifiers.
     *
     * @param pid the PID to delete.
     * @return true if the identifier was deleted, false if it did not exist.
     */
    public boolean deletePid(String pid) throws ExternalServiceException;

    /**
     * Returns all PIDs which are registered for the configured prefix.
     * 
     * The result may be very large, use carefully.
     * 
     * @return all PIDs which are registered for the configured prefix.
     */
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException;
}
