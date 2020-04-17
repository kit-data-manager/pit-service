/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.util;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Torridity
 */
public class TypeValidationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TypeValidationUtils.class);

    TypeValidationUtils() {
    }

    public static boolean isValid(PIDRecord record, TypeDefinition profile) {
        LOG.trace("Validating PID record against type definition.");
        if (!record.checkTypeConformance(profile)) {
            LOG.warn("PID record does not contain all required elements of type definition.");
            //invalid according to type
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
