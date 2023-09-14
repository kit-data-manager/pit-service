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

/**
 * Utility class with static functions to validate PID records.
 * 
 * @author Thomas Jejkal
 */
public class TypeValidationUtils {

    private TypeValidationUtils() {}

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
                    pidRecord,
                    "Missing mandatory types: " + missing);
        }
    }
}
