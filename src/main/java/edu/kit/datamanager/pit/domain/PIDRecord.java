/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 *
 * @author Torridity
 */
@Data
public class PIDRecord {

    private String pid;

    private Map<String, PIDRecordEntry> entries;

    public PIDRecord() {
        entries = new HashMap<>();
    }

    public void addEntry(String propertyIdentifier, String propertyName, String propertyValue) {
        if (propertyIdentifier.isEmpty()) {
            throw new IllegalArgumentException("The identifier of a property may not be empty!");
        }
        PIDRecordEntry entry = new PIDRecordEntry();
        entry.setKey(propertyIdentifier);
        entry.setName(propertyName);
        entry.setValue(propertyValue);
        entries.put(propertyIdentifier, entry);
    }

    @JsonIgnore
    public void setPropertyName(String propertyIdentifier, String name) {
        PIDRecordEntry pe = entries.get(propertyIdentifier);
        if (pe == null) {
            throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
        }
        pe.setName(name);
    }

    public boolean hasProperty(String propertyIdentifier) {
        return entries.containsKey(propertyIdentifier);
    }

    /**
     * Removes all properties that are not listed in the given collection.
     *
     * @param propertiesToKeep a collection of property identifiers to keep.
     */
    public void removePropertiesNotListed(Collection<String> propertiesToKeep) {
        Iterator<String> iter = entries.keySet().iterator();
        while (iter.hasNext()) {
            String propID = iter.next();
            if (!propertiesToKeep.contains(propID)) {
                iter.remove();
            }
        }
    }

    /**
     * Checks whether the stored property values conform to the given type and
     * stores the result of the conformance checks in the local information
     * record.
     *
     * @param typeDef
     * @return true if all mandatory properties of the type are present
     */
    public boolean checkTypeConformance(edu.kit.datamanager.pit.domain.TypeDefinition typeDef) {
        boolean conf = true;
        for (String p : typeDef.getAllProperties()) {
            if (!typeDef.getSubTypes().get(p).isOptional() && !entries.containsKey(p)) {
                conf = false;
                break;
            }
        }
        return conf;
    }

    @JsonIgnore
    public Set<String> getPropertyIdentifiers() {
        return entries.keySet();
    }

    public String getPropertyValue(String propertyIdentifier) {
        PIDRecordEntry entry = entries.get(propertyIdentifier);
        if (entry == null) {
            throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
        }
        return entry.getValue();
    }

}
