package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
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
 * 
 * - checks if all mandatory attributes are present
 * - validates all available attributes
 * - fails if an attribute is not defined within the profile
 */
public class EmbeddedStrictValidatorStrategy implements IValidationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedStrictValidatorStrategy.class);
    protected static final Executor EXECUTOR = Executors.newWorkStealingPool();

    protected ITypeRegistry typeRegistry;
    protected boolean additionalAttributesAllowed;
    protected Set<String> profileKeys;

    public EmbeddedStrictValidatorStrategy(
            ITypeRegistry typeRegistry,
            ApplicationProperties config
    ) {
        this.typeRegistry = typeRegistry;
        this.profileKeys = config.getProfileKeys();
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
                .map(attributePid -> this.typeRegistry.queryAttributeInfo(attributePid))
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
                    boolean isProfile = this.profileKeys.contains(attributeInfo.pid());
                    if (isProfile) {
                        Arrays.stream(pidRecord.getPropertyValues(attributeInfo.pid()))
                                .map(profilePid -> this.typeRegistry.queryAsProfile(profilePid))
                                .forEach(registeredProfileFuture -> registeredProfileFuture.thenApply(registeredProfile -> {
                                    registeredProfile.validateAttributes(pidRecord, this.additionalAttributesAllowed);
                                    return registeredProfile;
                                }));
                    }
                    return attributeInfo;
                }))
                .toList();


        try {
            CompletableFuture.allOf(attributeInfoFutures.toArray(new CompletableFuture<?>[0])).join();
        } catch (CompletionException e) {
            throwRecordValidationExceptionCause(e);
            throw new ExternalServiceException(this.typeRegistry.getRegistryIdentifier());
        } catch (CancellationException e) {
            throwRecordValidationExceptionCause(e);
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
    private static void throwRecordValidationExceptionCause(Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof RecordValidationException rve) {
            throw rve;
        } else if (cause != null && cause.getCause() instanceof RecordValidationException rve) {
            // in some cases we need to go deeper, because profiles are handled in a future within a future.
            throw rve;
        }
    }
}
