package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates a PID record using embedded profile(s).
 * <p>
 * - checks if all mandatory attributes are present
 * - validates all available attributes
 * - fails if an attribute is not defined within the profile
 */
public class EmbeddedStrictValidatorStrategy implements IValidationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedStrictValidatorStrategy.class);

    protected final ITypeRegistry typeRegistry;
    protected final boolean alwaysAcceptAdditionalAttributes;
    protected final Set<String> profileKeys;

    public EmbeddedStrictValidatorStrategy(
            ITypeRegistry typeRegistry,
            ApplicationProperties config
    ) {
        this.typeRegistry = typeRegistry;
        this.profileKeys = config.getProfileKeys();
        this.alwaysAcceptAdditionalAttributes = config.isValidationAlwaysAllowAdditionalAttributes();
    }

    @Override
    public void validate(PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException
    {
        if (pidRecord.getPropertyIdentifiers().isEmpty()) {
            throw new RecordValidationException(pidRecord, "Record is empty!");
        }

        // For each attribute in record, resolve schema and check the value
        List<CompletableFuture<AttributeInfo>> attributeInfoFutures = pidRecord.getPropertyIdentifiers().stream()
                // resolve attribute info (type and schema)
                .map(this.typeRegistry::queryAttributeInfo)
                // validate values using schema
                .map(attributeInfoFuture -> attributeInfoFuture.thenApply(attributeInfo -> {
                    for (String value : pidRecord.getPropertyValues(attributeInfo.pid())) {
                        boolean isValid = attributeInfo.validate(value);
                        if (!isValid) {
                            throw new RecordValidationException(
                                    pidRecord,
                                    "Attribute %s has a non-complying value %s"
                                            .formatted(attributeInfo.pid(), value));
                        }
                    }
                    return attributeInfo;
                }))
                // resolve profiles and apply their validation
                .map(attributeInfoFuture -> attributeInfoFuture.thenApply(attributeInfo -> {
                    boolean indicatesProfileValue = this.profileKeys.contains(attributeInfo.pid());
                    if (indicatesProfileValue) {
                        Arrays.stream(pidRecord.getPropertyValues(attributeInfo.pid()))
                                .map(this.typeRegistry::queryAsProfile)
                                .forEach(registeredProfileFuture -> registeredProfileFuture.thenApply(registeredProfile -> {
                                    registeredProfile.validateAttributes(pidRecord, this.alwaysAcceptAdditionalAttributes);
                                    return registeredProfile;
                                }));
                    }
                    return attributeInfo;
                }))
                .toList();


        try {
            LOG.trace("Processing all attributes in the record {}.", pidRecord.getPid());
            CompletableFuture.allOf(attributeInfoFutures.toArray(new CompletableFuture<?>[0])).join();
            LOG.trace("Finished processing all attributes in the record {}.", pidRecord.getPid());
        } catch (CompletionException e) {
            LOG.trace("Exception occurred during validation of record {}. Unpack Exception, if required.", pidRecord.getPid(), e);
            unpackAsyncExceptions(pidRecord, e);
            LOG.trace("Exception was not unpacked. Rethrowing.", e);
            throw new ExternalServiceException(this.typeRegistry.getRegistryIdentifier());
        } catch (CancellationException e) {
            unpackAsyncExceptions(pidRecord, e);
            throw new RecordValidationException(
                    pidRecord,
                    String.format("Validation task was cancelled for %s. Please report.", pidRecord.getPid()));
        }
    }

    /**
     * Checks Exceptions' causes for a RecordValidationExceptions, and throws them, if present.
     * <p>
     * Usually used to avoid exposing exceptions related to futures.
     * @param e the exception to unwrap.
     */
    private static void unpackAsyncExceptions(PIDRecord pidRecord, Throwable e) {
        final int MAX_LEVEL = 10;
        Throwable cause = e;

        for (int level = 0; level <= MAX_LEVEL && cause != null; level++) {
            cause = cause.getCause();
            if (cause instanceof RecordValidationException rve) {
                throw rve;
            } else if (cause instanceof TypeNotFoundException tnf) {
                throw new RecordValidationException(
                        pidRecord,
                        "Type not found: %s".formatted(tnf.getMessage()));
            }
        }
    }
}
