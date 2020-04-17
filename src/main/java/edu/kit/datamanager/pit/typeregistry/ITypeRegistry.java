package edu.kit.datamanager.pit.typeregistry;

import java.io.IOException;

import edu.kit.datamanager.pit.domain.TypeDefinition;

import java.net.URISyntaxException;

/**
 * Main abstraction interface towards the type registry. Contains all methods
 * required from the registry by the core services.
 *
 */
public interface ITypeRegistry {

    /**
     * Queries a type definition record from the type registry.
     *
     * @param typeIdentifier
     * @return a type definition record or null if the type is not registered.
     * @throws IOException on communication errors with a remote registry
     */
    public TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException, URISyntaxException;

    /**
     * Determines whether the given PID is an identifier in the type registry.
     * Note that a positive answer does not necessarily mean the identifier is
     * registered and resolvable.
     *
     * @param pid
     * @return true if the PID is generally acceptable for the type registry,
     * but may still be unregistered.
     */
    public boolean isTypeRegistryPID(String pid);

}
