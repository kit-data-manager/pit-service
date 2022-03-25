/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private Map<String, List<PIDRecordEntry>> entries;

    public PIDRecord() {
        this.entries = new HashMap<>();
    }

    // Convenience setter / builder method.
    public PIDRecord withPID(String pid) {
        this.setPid(pid);
        return this;
    }

    public void addEntry(String propertyIdentifier, String propertyName, String propertyValue) {
        if (propertyIdentifier.isEmpty()) {
            throw new IllegalArgumentException("The identifier of a property may not be empty!");
        }
        PIDRecordEntry entry = new PIDRecordEntry();
        entry.setKey(propertyIdentifier);
        entry.setName(propertyName);
        entry.setValue(propertyValue);
        List<PIDRecordEntry> entryList = this.entries.get(propertyIdentifier);
        if (entryList == null) {
            entryList = new ArrayList<>();
            entries.put(propertyIdentifier, entryList);
        }
        entryList.add(entry);
    }

    @JsonIgnore
    public void setPropertyName(String propertyIdentifier, String name) {
        List<PIDRecordEntry> pe = entries.get(propertyIdentifier);
        if (pe == null) {
            throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
        }
        for (PIDRecordEntry entry : pe) {
            entry.setName(name);
        }
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
     * Checks if all mandatory properties of a type (or profile) are available in
     * this PID record.
     *
     * @param typeDef the given type or profile definition.
     * @return true if all mandatory properties of the type are present.
     */
    public boolean checkTypeConformance(edu.kit.datamanager.pit.domain.TypeDefinition typeDef) {
        // TODO Validation should be externalized, so validation strategies can be exchanged.
        // TODO Validation should be kept in one place, e.g. a special module.
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

    /**
     * Get the value of the first element in case there are multiple elements
     * for the provided propertyIndentifier.
     */
    public String getPropertyValue(String propertyIdentifier) {
        List<PIDRecordEntry> entry = entries.get(propertyIdentifier);
        if (entry == null) {
            throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
        }
        return entry.get(0).getValue();
    }

    public String[] getPropertyValues(String propertyIdentifier) {
        List<PIDRecordEntry> entry = entries.get(propertyIdentifier);
        if (entry == null) {
            throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
        }

        List<String> values = new ArrayList<>();
        for (PIDRecordEntry e : entry) {
            values.add(e.getValue());
        }
        return values.toArray(new String[] {});
    }
}
