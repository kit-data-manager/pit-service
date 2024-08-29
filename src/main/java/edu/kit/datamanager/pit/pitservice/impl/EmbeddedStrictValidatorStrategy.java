package edu.kit.datamanager.pit.pitservice.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.util.TypeValidationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    public AsyncLoadingCache<String, TypeDefinition> typeLoader;

    @Autowired
    ApplicationProperties applicationProps;

    @Override
    public void validate(PIDRecord pidRecord) throws RecordValidationException, ExternalServiceException {
        String profileKey = applicationProps.getProfileKey();
        if (!pidRecord.hasProperty(profileKey)) {
            throw new RecordValidationException(
                    pidRecord,
                    "Profile attribute not found. Expected key: " + profileKey);
        }

        String[] profilePIDs = pidRecord.getPropertyValues(profileKey);
        boolean hasProfile = profilePIDs.length > 0;
        if (!hasProfile) {
            throw new RecordValidationException(
                    pidRecord,
                    "Profile attribute " + profileKey + " has no values.");
        }

        List<CompletableFuture<?>> futures = Streams.stream(Arrays.stream(profilePIDs))
                .map(profilePID -> {
                    try {
                        return this.typeLoader.get(profilePID)
                                .thenAcceptAsync(profileDefinition -> {
                                    if (profileDefinition == null) {
                                        LOG.error("No type definition found for identifier {}.", profilePID);
                                        throw new RecordValidationException(
                                                pidRecord,
                                                String.format("No type found for identifier %s.", profilePID));
                                    }
                                    this.strictProfileValidation(pidRecord, profileDefinition);
                                }, EXECUTOR);
                    } catch (RuntimeException e) {
                        LOG.error("Could not resolve identifier {}.", profilePID);
                        throw new ExternalServiceException(
                                applicationProps.getTypeRegistryUri().toString());
                    }
                })
                .collect(Collectors.toList());
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        } catch (CompletionException e) {
            throwRecordValidationExceptionCause(e);
            throw new ExternalServiceException(
                    applicationProps.getTypeRegistryUri().toString());
        } catch (CancellationException e) {
            throwRecordValidationExceptionCause(e);
            throw new RecordValidationException(
                    pidRecord,
                    String.format("Validation task was cancelled for %s. Please report.", pidRecord.getPid()));
        }
    }

    private static void throwRecordValidationExceptionCause(Throwable e) {
        if (e.getCause() instanceof RecordValidationException rve) {
            throw rve;
        }
    }

    /**
     * Exceptions indicate failure. No Exceptions mean success.
     * 
     * @param pidRecord the PID record to validate.
     * @param profile   the profile to validate against.
     * @throws RecordValidationException with error message on validation errors.
     */
    private void strictProfileValidation(PIDRecord pidRecord, TypeDefinition profile) throws RecordValidationException {
        // if (profile.hasSchema()) {
        // TODO issue https://github.com/kit-data-manager/pit-service/issues/104
        // validate using schema and you are done (strict validation)
        // String jsonRecord = ""; // TODO format depends on schema source
        // return profile.validate(jsonRecord);
        // }

        LOG.trace("Validating PID record against profile {}.", profile.getIdentifier());

        TypeValidationUtils.checkMandatoryAttributes(pidRecord, profile);

        for (String attributeKey : pidRecord.getPropertyIdentifiers()) {
            LOG.trace("Checking PID record key {}.", attributeKey);

            TypeDefinition type = profile.getSubTypes().get(attributeKey);
            if (type == null) {
                LOG.error("No sub-type found for key {}.", attributeKey);
                // TODO try to resolve it (for later when we support "allow additional
                // attributes")
                // if profile.allowsAdditionalAttributes() {...} else
                throw new RecordValidationException(
                        pidRecord,
                        String.format("Attribute %s is not allowed in profile %s",
                                attributeKey,
                                profile.getIdentifier()));
            }

            validateValuesForKey(pidRecord, attributeKey, type);
        }
        LOG.debug("successfully validated {}", profile.getIdentifier());
    }

    /**
     * Validates all values of an attribute against a given type definition.
     * 
     * @param pidRecord the record containing the attribute and value.
     * @param attributeKey the attribute to check the values for.
     * @param type the type definition to check against.
     * @throws RecordValidationException on error.
     */
    private void validateValuesForKey(PIDRecord pidRecord, String attributeKey, TypeDefinition type)
            throws RecordValidationException {
        String[] values = pidRecord.getPropertyValues(attributeKey);
        for (String value : values) {
            if (value == null) {
                LOG.error("'null' record value found for key {}.", attributeKey);
                throw new RecordValidationException(
                        pidRecord,
                        String.format("Validation of value %s against type %s failed.",
                                value,
                                type.getIdentifier()));
            }

            if (!type.validate(value)) {
                LOG.error("Validation of value {} against type {} failed.", value, type.getIdentifier());
                throw new RecordValidationException(
                        pidRecord,
                        String.format("Validation of value %s against type %s failed.",
                                value,
                                type.getIdentifier()));
            }
        }
    }
}
