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
}
