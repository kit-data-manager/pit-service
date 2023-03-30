package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.kit.datamanager.pit.pidsystem.impl.local.PidDatabaseObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The internal representation for a PID record, offering methods to manipulate
 * the record.
 * 
 * While other representations exist, they are only used for easier database
 * communication or representation for the outside. In contrast, this is the
 * internal representation offering methods for manipulation.
 */
public class PIDRecord {

    private String pid = "";

    private Map<String, List<PIDRecordEntry>> entries = new HashMap<>();

    /**
     * Creates an empty record without PID.
     */
    public PIDRecord() {}

    /**
     * Creates a record with the same content as the given representation.
     * 
     * @param dbo the given record representation.
     */
    public PIDRecord(PidDatabaseObject dbo) {
        this.setPid(dbo.getPid());
        dbo.getEntries().entrySet().stream().forEach(entry -> {
            String key = entry.getKey();
            entry
                .getValue() // ArrayList<String>
                .stream()
                .forEach( value -> this.addEntry(key, "", value) );
        });
    }

    public PIDRecord(SimplePidRecord rec) {
        this.entries = new HashMap<>();
        for (SimplePair pair : rec.getPairs()) {
            this.addEntry(pair.getKey(), "", pair.getValue());
        }
    }

    /**
     * Convenience setter / builder method.
     * 
     * @param pid the pid to set in this object.
     * @return this object (builder method).
     */
    public PIDRecord withPID(String pid) {
        this.setPid(pid);
        return this;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Map<String, List<PIDRecordEntry>> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, List<PIDRecordEntry>> entries) {
        this.entries = entries;
    }

    /**
     * Adds a new key-name-value triplet.
     * 
     * @param propertyIdentifier the key/type PID.
     * @param propertyName the human-readable name for the given key/type.
     * @param propertyValue the value to this key/type.
     */
    public void addEntry(String propertyIdentifier, String propertyName, String propertyValue) {
        if (propertyIdentifier.isEmpty()) {
            throw new IllegalArgumentException("The identifier of a property may not be empty!");
        }
        PIDRecordEntry entry = new PIDRecordEntry();
        entry.setKey(propertyIdentifier);
        entry.setName(propertyName);
        entry.setValue(propertyValue);

        this.entries
            .computeIfAbsent(propertyIdentifier, key -> new ArrayList<>())
            .add(entry);
    }

    /**
     * Sets the name for a given key/type in all available pairs.
     * 
     * @param propertyIdentifier the key/type.
     * @param name the new name.
     */
    @JsonIgnore
    public void setPropertyName(String propertyIdentifier, String name) {
        List<PIDRecordEntry> propertyEntries = this.entries.get(propertyIdentifier);
        if (propertyEntries == null) {
            throw new IllegalArgumentException(
                "Property identifier not listed in this record: " + propertyIdentifier);
        }
        for (PIDRecordEntry entry : propertyEntries) {
            entry.setName(name);
        }
    }

    /**
     * Check if there is a pair or triplet containing the given property (key/type)
     * is availeble in this record.
     * 
     * @param propertyIdentifier the key/type to search for.
     * @return true, if the property/key/type is present.
     */
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
     * Returns all missing mandatory attributes from the given Profile, which are not
     * present in this record.
     * 
     * @param profile the given Profile definition.
     * @return all missing mandatory attributes.
     */
    public Collection<String> getMissingMandatoryTypesOf(TypeDefinition profile) {
        Collection<String> missing = new ArrayList<>();
        for (TypeDefinition td : profile.getSubTypes().values()) {
            String typePid = td.getIdentifier();
            if (!td.isOptional() && !this.entries.containsKey(typePid)) {
                missing.add(typePid);
            }
        }
        return missing;
    }

    /**
     * Get all properties contained in this record.
     * 
     * @return al contained properties.
     */
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

    /**
     * Get all values of a given property.
     * 
     * @param propertyIdentifier the given property identifier.
     * @return all values of the given property.
     */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((entries == null) ? 0 : entries.hashCode());
        return result;
    }

    /**
     * Checks if two PIDRecords are equivalent.
     * 
     * - Ignores the name attribute: Only keys and values matter.
     * - Ignores order of keys or values
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null) {return false;}
        if (getClass() != obj.getClass()) {return false;}

        PIDRecord other = (PIDRecord) obj;
        if (pid == null) {
            if (other.pid != null) {return false;}
        } else if (!pid.equals(other.pid)) {
            return false;
        }

        if (this.entries == null) {
            return other.entries == null;
        } else if (other.entries == null) {
            return this.entries == null;
        } else {
            // Equal means:
            // 1. have the same set of keys
            boolean isEqual = this.entries.keySet().equals(other.getEntries().keySet());
            if (!isEqual) {return false;}
            // 2. for each key, have the same values (order does not matter)
            isEqual &= this.entries.values().stream()
                .flatMap(List<PIDRecordEntry>::stream)
                .filter(entry -> other.entries.get(entry.getKey()).stream()
                    .filter(otherEntry -> otherEntry.getValue().equals(entry.getValue()))
                    .count() == 0 // keep key-value-pairs with values not present in `other`.
                )
                .count() == 0; // there should be no pairs with values which are not available in `other`.
            return isEqual;
        }
    }

    @Override
    public String toString() {
        return "PIDRecord [pid=" + pid + ", entries=" + entries + "]";
    }
}
