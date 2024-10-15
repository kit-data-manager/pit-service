package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.kit.datamanager.entities.EtagSupport;
import edu.kit.datamanager.pit.pidsystem.impl.local.PidDatabaseObject;

import javax.annotation.CheckReturnValue;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The internal representation for a PID record, offering methods to manipulate
 * the record.
 * <p>
 * While other representations exist, they are only used for easier database
 * communication or representation for the outside. In contrast, this is the
 * internal representation offering methods for manipulation.
 */
public record PIDRecord(
        String pid,
        Map<String, List<PidRecordEntry>> entries
) implements EtagSupport {

    /**
     * Creates an empty record without PID.
     */
    public PIDRecord() {
        this("", new HashMap<>());
    }

    /**
     * Creates a record with the same content as the given representation.
     * 
     * @param dbo the given record representation.
     */
    public PIDRecord(PidDatabaseObject dbo) {
        this(
                dbo.getPid(),
                dbo.getEntries().entrySet().stream()
                        .map(entry -> new AbstractMap.SimpleEntry<String, ArrayList<PidRecordEntry>>(
                                entry.getKey(),
                                entry.getValue().stream()
                                        .map(value -> new PidRecordEntry(entry.getKey(), "", value))
                                        .collect(Collectors.toCollection(ArrayList::new)))
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public PIDRecord(SimplePidRecord rec) {
        this(
                rec.pid(),
                rec.pairs().stream()
                        .map(simplePair -> new AbstractMap.SimpleEntry<String, PidRecordEntry>(
                                simplePair.key(),
                                new PidRecordEntry(simplePair.key(), "", simplePair.value())
                        ))
                        .collect(Collectors.toMap(
                                AbstractMap.SimpleEntry::getKey,
                                e -> new ArrayList<>(List.of(e.getValue())),
                                (e1, e2) -> {e1.addAll(e2); return e1;}
                        ))
        );
    }

    /**
     * Convenience setter / builder method.
     * 
     * @param pid the pid to set in this object.
     * @return this object (builder method).
     */
    @CheckReturnValue
    public PIDRecord withPID(String pid) {
        return new PIDRecord(pid, new HashMap<>(entries));
    }

    @JsonIgnore
    public Set<SimplePair> getSimpleEntries() {
        return new HashSet<>(new SimplePidRecord(this).pairs());
    }

    @CheckReturnValue
    public PIDRecord addEntry(String propertyIdentifier, String propertyValue) {
        return this.addEntry(propertyIdentifier, "", propertyValue);
    }

    /**
     * Returns a copy of this instance, but with an additional entry.
     * 
     * @param propertyIdentifier the key/type PID.
     * @param propertyName the human-readable name for the given key/type.
     * @param propertyValue the value to this key/type.
     */
    @CheckReturnValue
    public PIDRecord addEntry(String propertyIdentifier, String propertyName, String propertyValue) {
        if (propertyIdentifier.isEmpty()) {
            throw new IllegalArgumentException("The identifier of a property may not be empty!");
        }
        PidRecordEntry entry = new PidRecordEntry(
                propertyIdentifier,
                propertyName,
                propertyValue
        );
        Map<String, List<PidRecordEntry>> entries = new HashMap<>(this.entries);
        entries.computeIfAbsent(propertyIdentifier, key -> new ArrayList<>())
            .add(entry);
        return new PIDRecord(pid, entries);
    }

    /**
     * Sets the name for a given key/type in all available pairs.
     * 
     * @param propertyIdentifier the key/type.
     * @param name the new name.
     */
    @JsonIgnore
    public void setPropertyName(String propertyIdentifier, String name) {
        List<PidRecordEntry> propertyEntries = this.entries.get(propertyIdentifier);
        if (propertyEntries == null) {
            throw new IllegalArgumentException(
                "Property identifier not listed in this record: " + propertyIdentifier);
        }
        this.entries.put(propertyIdentifier, propertyEntries.stream()
                .map(entry -> entry.withName(name))
                .toList());
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
    @CheckReturnValue
    public PIDRecord removePropertiesNotListed(Collection<String> propertiesToKeep) {
        HashMap<String, List<PidRecordEntry>> entries = new HashMap<>(this.entries);
        entries.keySet().removeIf(propID -> !propertiesToKeep.contains(propID));
        return new PIDRecord(pid, entries);
    }

    @CheckReturnValue
    public PIDRecord removeAllValuesOf(String attribute) {
        Map<String, List<PidRecordEntry>> entries = new HashMap<>(this.entries);
        entries.remove(attribute);
        return new PIDRecord(pid, entries);
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
        return new HashSet<>(entries.keySet());
    }

    /**
     * Get the value of the first element in case there are multiple elements
     * for the provided propertyIdentifier.
     */
    public String getPropertyValue(String propertyIdentifier) {
        List<PidRecordEntry> entry = entries.get(propertyIdentifier);
        if (entry == null) {
            return "";
        }
        return entry.getFirst().value();
    }

    /**
     * Get all values of a given property.
     * 
     * @param propertyIdentifier the given property identifier.
     * @return all values of the given property.
     */
    public List<String> getPropertyValues(String propertyIdentifier) {
        List<PidRecordEntry> entry = entries.get(propertyIdentifier);
        if (entry == null) { return List.of(); }
        return entry.stream()
                .map(PidRecordEntry::value)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        Set<SimplePair> simpleEntries = this.getSimpleEntries();
        result = prime * result + ((simpleEntries == null) ? 0 : simpleEntries.hashCode());
        return result;
    }

    /**
     * Checks if two PIDRecords are equivalent.
     * <p>
     * - Ignores the name attribute: Only keys and values matter.
     * - Ignores order of keys or values
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null) {return false;}
        if (getClass() != obj.getClass()) {return false;}

        PIDRecord other = (PIDRecord) obj;
        boolean isThisPidEmpty = pid == null || pid.isBlank();
        boolean isOtherPidEmpty = other.pid == null || other.pid.isBlank();
        boolean isBothPidEmpty = isThisPidEmpty && isOtherPidEmpty;
        boolean equalPIDs = isBothPidEmpty || (this.pid != null && this.pid.equals(other.pid));

        if (!equalPIDs) {
            return false;
        }

        // this ignores attributes order, names, and even duplicates
        return this.getSimpleEntries().equals(other.getSimpleEntries());
    }

    /**
     * Calculates an etag for a record.
     * 
     * @return an etag, which is independent of any order or duplicates in the
     *         entries.
     */
    @JsonIgnore
    @Override
    public String getEtag() {
        return Integer.toString(this.hashCode());
    }
}
