/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.pit.configuration;

import edu.kit.datamanager.configuration.GenericApplicationProperties;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.pitservice.impl.EmbeddedStrictValidatorStrategy;
import edu.kit.datamanager.pit.pitservice.impl.NoValidationStrategy;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * The main properties a user can give to this service using a
 * application.properties file.
 * <p>
 * Depending on the configuration, further configuration classes might be
 * loaded,
 * to give the user mode operions.
 * <p>
 * Example: If "pit.pidsystem.implementation" is "HANDLE_PROTOCOL" is set,
 * `HandleProtocolProperties` will be active.
 *
 * @author Andreas Pfeil
 */
@Configuration
@Validated
public class ApplicationProperties extends GenericApplicationProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationProperties.class);

    /**
     * Internal default set of types which indicate that, when used as a key
     * of an attribute, that the value of the attribute must be a profile.
     * Used for profile detection in records.
     */
    private static final Set<String> KNOWN_PROFILE_KEYS = Set.of(
            "21.T11148/076759916209e5d62bd5",
            "21.T11969/bcc54a2a9ab5bf2a8f2c"
    );
    @Value("#{${pit.validation.profileKeys:{}}}")
    @NotNull
    protected List<String> profileKeys = List.of();
    @Value("${pit.pidsystem.implementation}")
    @NotNull
    private IdentifierSystemImpl identifierSystemImplementation;
    @Value("${pit.validation.strategy:embedded-strict}")
    @NotNull
    private ValidationStrategy validationStrategy = ValidationStrategy.EMBEDDED_STRICT;
    @Value("${pit.storage.strategy:keep-modified}")
    @NotNull
    private StorageStrategy storageStrategy = StorageStrategy.KEEP_MODIFIED;
    // TODO Used by DTR implementation for resolving. Too unflexible in mid-term.
    @Value("${pit.pidsystem.handle.baseURI}")
    private URL handleBaseUri;
    @Value("${pit.typeregistry.baseURI}")
    private URL typeRegistryUri;
    @Value("${pit.typeregistry.cache.maxEntries:1000}")
    private int cacheMaxEntries;
    @Value("${pit.typeregistry.cache.lifetimeMinutes:10}")
    private long cacheExpireAfterWriteLifetime;
    @Value("${pit.validation.profileKey:21.T11148/076759916209e5d62bd5}")
    @Deprecated(forRemoval = true /*In Typed PID Maker 3.0.0*/)
    private String profileKey;
    @Getter
    @Setter
    @Value("${pit.validation.alwaysAllowAdditionalAttributes:true}")
    private boolean validationAlwaysAllowAdditionalAttributes = true;
    @Getter
    @Setter
    @Value("${pit.observability.includePiiInTraces:false}")
    private boolean includePiiInTraces = false;

    @Bean
    public IValidationStrategy defaultValidationStrategy(ITypeRegistry typeRegistry) {
        IValidationStrategy defaultStrategy = new NoValidationStrategy();
        if (this.validationStrategy == ValidationStrategy.EMBEDDED_STRICT) {
            defaultStrategy = new EmbeddedStrictValidatorStrategy(typeRegistry, this);
        }
        return defaultStrategy;
    }

    public @NotNull Set<String> getProfileKeys() {
        Set<String> allProfileKeys = new java.util.HashSet<>(Set.copyOf(KNOWN_PROFILE_KEYS));
        allProfileKeys.addAll(profileKeys);
        allProfileKeys.add(this.getProfileKey());
        return allProfileKeys;
    }

    public void setProfileKeys(@NotNull List<String> profileKeys) {
        this.profileKeys = profileKeys;
    }

    @Deprecated(forRemoval = true)
    public String getProfileKey() {
        return this.profileKey;
    }

    @Deprecated(forRemoval = true)
    public void setProfileKey(String profileKey) {
        this.profileKey = profileKey;
    }

    public IdentifierSystemImpl getIdentifierSystemImplementation() {
        return this.identifierSystemImplementation;
    }

    public void setIdentifierSystemImplementation(IdentifierSystemImpl identifierSystemImplementation) {
        this.identifierSystemImplementation = identifierSystemImplementation;
    }

    public URL getHandleBaseUri() {
        return this.handleBaseUri;
    }

    public void setHandleBaseUri(URL handleBaseUri) {
        this.handleBaseUri = handleBaseUri;
    }

    public URL getTypeRegistryUri() {
        return this.typeRegistryUri;
    }

    public void setTypeRegistryUri(URL typeRegistryUri) {
        this.typeRegistryUri = typeRegistryUri;
    }

    public ValidationStrategy getValidationStrategy() {
        return this.validationStrategy;
    }

    public void setValidationStrategy(ValidationStrategy strategy) {
        this.validationStrategy = strategy;
    }

    public int getCacheMaxEntries() {
        if (this.cacheMaxEntries <= 10) {
            LOG.warn("Cache max entries is set to {} (low value)", this.cacheMaxEntries);
        }
        return this.cacheMaxEntries;
    }

    public void setCacheMaxEntries(int cacheMaxEntries) {
        this.cacheMaxEntries = cacheMaxEntries;
    }

    public long getCacheExpireAfterWriteLifetime() {
        return cacheExpireAfterWriteLifetime;
    }

    public void setCacheExpireAfterWriteLifetime(long cacheExpireAfterWriteLifetime) {
        this.cacheExpireAfterWriteLifetime = cacheExpireAfterWriteLifetime;
    }

    public StorageStrategy getStorageStrategy() {
        return storageStrategy;
    }

    public void setStorageStrategy(StorageStrategy storageStrategy) {
        this.storageStrategy = storageStrategy;
    }

    public enum IdentifierSystemImpl {
        IN_MEMORY,
        LOCAL,
        HANDLE_PROTOCOL
    }

    public enum ValidationStrategy {
        EMBEDDED_STRICT,
        NONE_DEBUG
    }

    public enum StorageStrategy {
        // Only store PIDs which have been created or modified using this instance
        KEEP_MODIFIED,
        // Store created, modified or resolved PIDs.
        KEEP_RESOLVED_AND_MODIFIED;

        public boolean storesModified() {
            return this == StorageStrategy.KEEP_MODIFIED
                    || this == StorageStrategy.KEEP_RESOLVED_AND_MODIFIED;
        }

        public boolean storesResolved() {
            return this == StorageStrategy.KEEP_RESOLVED_AND_MODIFIED;
        }
    }
}
