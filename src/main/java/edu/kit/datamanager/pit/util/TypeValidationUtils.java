/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.util;

import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class with static functions to validate PID records.
 * 
 * @author Thomas Jejkal
 */
public class TypeValidationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TypeValidationUtils.class);

    /**
     * Check if all mandatory attributes are present. If not, throw an exception.
     * 
     * @param pidRecord the record to check for
     * @param profile   the profile to check against
     * @throws RecordValidationException if at least one attribute is missing. It
     *                                   shows all missing attributes in its error
     *                                   message.
     */
    public static void checkMandatoryAttributes(PIDRecord pidRecord, TypeDefinition profile)
            throws RecordValidationException {
        Collection<String> missing = pidRecord.getMissingMandatoryTypesOf(profile);
        if (!missing.isEmpty()) {
            throw new RecordValidationException(
                    pidRecord.getPid(),
                    "Missing mandatory types: " + missing);
        }
    }

    /**
     * Validates a PID record against a given profile.
     * 
     * - All mandatory properties of the profile must be available in the PID
     * record.
     * - All properties of the record must be successfully validated according to
     * the profile subtypes (properties).
     * 
     * @param record  the record to validate.
     * @param profile the profile to validate against, defining the rules for the
     *                record.
     * @return true if all validations were successful, false otherwise.
     */
    public static boolean isValid(PIDRecord record, TypeDefinition profile) {
        LOG.trace("Validating PID record against type definition.");
        if (!pidRecord.getMissingMandatoryTypesOf(profile).isEmpty()) {
            LOG.warn("PID record does not contain all required elements of type definition.");
            // invalid according to type
            return false;
        }
        for (String recordKey : record.getPropertyIdentifiers()) {
            LOG.trace("Checking PID record key {}.", recordKey);
            TypeDefinition type = profile.getSubTypes().get(recordKey);
            if (type == null) {
                LOG.error("No sub-type found for key {}.", recordKey);
                return false;
            }

            String[] values = record.getPropertyValues(recordKey);
            for (String value : values) {
                if (value == null) {
                    LOG.error("'null' record value found for key {}.", recordKey);
                    return false;
                }

                if (!type.validate(value)) {
                    LOG.error("Validation of value {} against type {} failed.", value, type.getIdentifier());
                    return false;
                }
            }
        }
        LOG.trace("PID record is matching the provided type definition.");
        return true;
    }
}
