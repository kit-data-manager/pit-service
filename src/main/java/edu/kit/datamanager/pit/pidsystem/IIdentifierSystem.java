package edu.kit.datamanager.pit.pidsystem;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import java.io.IOException;
import java.util.Map;

/**
 * Main abstraction interface towards the identifier system containing
 * registered identifiers and associated state information.
 *
 */
public interface IIdentifierSystem {

    /**
     * Checks whether the given PID is already registered.
     *
     * @param pid
     * @return True or false
     * @throws IOException
     */
    public boolean isIdentifierRegistered(String pid) throws IOException;

    /**
     * Queries all properties from the given PID, independent of types.
     *
     * @param pid
     * @return a PID information record with property identifiers mapping to
     * values. The property names will be empty strings. Contains all property
     * values present in the record of the given PID. If the pid is not
     * registered, the method returns null.
     * @throws IOException
     */
    public PIDRecord queryAllProperties(String pid) throws IOException;

    /**
     * Queries a single property from the given PID.
     *
     * @param pid
     * @param typeDefinition
     * @return the property value or null if there is no property of given name
     * defined in this PID record.
     * @throws IOException
     */
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException;

    /**
     * Registers a new PID with given property values. The method decides on a
     * PID name automatically, guaranteeing its uniqueness and preventing
     * failure due to potential overwrites.
     *
     * @param properties A simple dictionary with string keys and string values
     * that contains the initial PID record.
     * @return the name of the registered PID
     * @throws IOException
     */
    public String registerPID(Map<String, String> properties) throws IOException;

    /**
     * Queries all properties of a given type available from the given PID. If
     * optional properties are present, they will be returned as well. If there
     * are mandatory properties missing (i.e. the record of the given PID does
     * not fully conform to the type), the method will NOT fail but simply
     * return only those properties that are present.
     *
     * @param pid
     * @param typeDefinition
     * @return a PID information record with property identifiers mapping to
     * values. The property names will not be available (empty Strings).
     * Contains all property values present in the record of the given PID that
     * are also specified by the type (mandatory or optional). If the pid is not
     * registered, the method returns null.
     * @throws IOException
     */
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException;

    /**
     * Remove the given PID. Obviously, this method is only for testing
     * purposes, since we should not delete persistent identifiers...
     *
     * @param pid
     * @return true if the identifier was deleted, false if it did not exist
     */
    public boolean deletePID(String pid);

}
