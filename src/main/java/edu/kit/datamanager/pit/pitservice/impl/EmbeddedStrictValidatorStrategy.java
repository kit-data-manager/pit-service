package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.util.TypeValidationUtils;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.LoadingCache;

/**
 * Validates a PID record using embedded profile(s).
 * 
 * - checks if all mandatory attributes are present
 * - validates all available attributes
 * - fails if an attribute is not defined within the profile
 */
public class EmbeddedStrictValidatorStrategy implements IValidationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedStrictValidatorStrategy.class);

    @Autowired
    public LoadingCache<String, TypeDefinition> typeLoader;

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

        for (String profilePID : profilePIDs) {
            TypeDefinition profileDefinition;
            try {
                profileDefinition = this.typeLoader.get(profilePID);
            } catch (ExecutionException e) {
                LOG.error("Could not resolve identifier {}.", profilePID);
                throw new ExternalServiceException(
                        applicationProps.getTypeRegistryUri().toString());
            }
            if (profileDefinition == null) {
                LOG.error("No type definition found for identifier {}.", profilePID);
                throw new RecordValidationException(
                        pidRecord,
                        String.format("No type found for identifier %s.", profilePID));
            }

            LOG.debug("validating profile {}", profilePID);
            this.strictProfileValidation(pidRecord, profileDefinition);
            LOG.debug("successfully validated {}", profilePID);
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

        LOG.trace("Validating PID record against type definition.");

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
