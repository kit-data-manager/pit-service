package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.common.InconsistentRecordsException;
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

//**
//   * Retrieves a property definition
//   *
//   * @param propertyIdentifier
//   * @return null if there is no property with given identifier, the definition
//   * record otherwise.
//   * @throws IOException
//   */
//  public PropertyDefinition describeProperty(String propertyIdentifier) throws IOException;
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

//    /**
//     * Resolves the given PID without previous knowledge about the kind of
//     * entity it identifies (e.g. a common PID record, a property or type
//     * definition etc.).
//     *
//     * @param pid
//     * @return The returned object can be either a PID record, a property
//     * definition or a type definition. It can also be null, indicating the PID
//     * is not registered at all.
//     * @throws IOException
//     */
//    public Object genericResolve(String pid) throws IOException;
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

    public PIDRecord queryAllProperties(String pid, boolean includePropertyNames) throws IOException, InconsistentRecordsException;

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
     * @throws InconsistentRecordsException
     */
    public PIDRecord queryByType(String pid, String typeIdentifier, boolean includePropertyNames) throws IOException, InconsistentRecordsException;

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
     * @throws InconsistentRecordsException
     */
    public PIDRecord queryByTypeWithConformance(String pid, String typeIdentifier, boolean includePropertyNames) throws IOException,
            InconsistentRecordsException;

    public PIDRecord queryByTypeWithConformance(String pid, List<String> typeIdentifiers, boolean includePropertyNames) throws IOException,
            InconsistentRecordsException;

//  /**
//   * Determines whether the given identifier references a simple object, a
//   * property, a type etc. Note that the method may contact a remote registry to
//   * answer the request since information may not be encoded in the identifier
//   * string.
//   *
//   * @param identifier
//   * @return a value of {@link EntityClass}
//   * @throws IOException
//   */
//  public EntityClass determineEntityClass(String identifier) throws IOException;

    /**
     * Returns an operations instance, configured with this typingService.
     * 
     * Convenience method for `new Operations(typingService)`.
     * 
     * @return an operation instance.
     */
    public Operations getOperations();
}
