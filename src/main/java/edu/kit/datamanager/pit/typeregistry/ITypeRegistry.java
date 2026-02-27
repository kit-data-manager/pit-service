package edu.kit.datamanager.pit.typeregistry;

import java.util.concurrent.CompletableFuture;

/**
 * Main abstraction interface towards the type registry. Contains all methods
 * required from the registry by the core services.
 *
 */
public interface ITypeRegistry {
    CompletableFuture<AttributeInfo> queryAttributeInfo(String attributePid);
    CompletableFuture<RegisteredProfile> queryAsProfile(String profilePid);

    /**
     * An identifier for exceptions and debugging purposes.
     *
     * @return a name ur url string that identifies the implementation or configuration well in case of errors.
     */
    String getRegistryIdentifier();
}
