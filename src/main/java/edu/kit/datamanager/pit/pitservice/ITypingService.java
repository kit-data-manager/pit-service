package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import java.io.IOException;
import java.util.List;

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
     * Retrieves a type definition
     *
     * @param typeIdentifier
     * @return null if there is no type with given identifier, the definition
     * record otherwise.
     * @throws IOException
     */
    public TypeDefinition describeType(String typeIdentifier) throws IOException;

    public boolean conformsToType(String pid, String typeIdentifier) throws IOException;

    /**
     * Queries a single property from the PID.
     *
     * @param pid
     * @param propertyIdentifier must be registered in the type registry
     * @return a PIDRecord object containing the single property name and
     * value or null if the property is undefined.
     * @throws IOException
     * @throws IllegalArgumentException if the property is defined but ambiguous
     * (type registry query returned multiple results).
     */
    public PIDRecord queryProperty(String pid, String propertyIdentifier) throws IOException;

    public PIDRecord queryAllProperties(String pid, boolean includePropertyNames) throws IOException;

    /**
     * Queries all properties of a type available from the given PID. If
     * optional properties are present, they will be returned as well. If there
     * are mandatory properties missing (i.e. the record of the given PID does
     * not fully conform to the type), the method will NOT fail but simply
     * return only those properties that are present.
     *
     * @param pid
     * @param typeIdentifier a type identifier, not a name
     * @param includePropertyNames if true, the method will also return property
     * names at additional call costs.
     * @return a PID information record with property identifiers mapping to
     * values. Contains all property values present in the record of the given
     * PID that are also specified by the type (mandatory or optional). If the
     * pid is not registered, the method returns null.
     * @throws IOException
     */
    public PIDRecord queryByType(String pid, String typeIdentifier, boolean includePropertyNames) throws IOException;

    /**
     * Same as {@link #queryByType}, but also performs a type conformance check.
     * The result of the check will be available from the PID information record
     * returned.
     *
     * @param pid
     * @param typeIdentifier
     * @param includePropertyNames
     * @return a PID information record with property information and
     * conformance check results.
     * @throws IOException
     */
    public PIDRecord queryByTypeWithConformance(String pid, String typeIdentifier, boolean includePropertyNames) throws IOException;

    public PIDRecord queryByTypeWithConformance(String pid, List<String> typeIdentifiers, boolean includePropertyNames) throws IOException;

    /**
     * Returns an operations instance, configured with this typingService.
     * 
     * Convenience method for `new Operations(typingService)`.
     * 
     * @return an operation instance.
     */
    public Operations getOperations();
}
