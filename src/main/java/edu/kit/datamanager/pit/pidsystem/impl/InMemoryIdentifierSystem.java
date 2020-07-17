package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

/**
 * A simple basis for demonstrations or tests of the service.
 * PIDs will be stored in a HashMap and not stored anywhere else.
 */
public class InMemoryIdentifierSystem implements IIdentifierSystem {
    private Map<String, PIDRecord> records;

    public InMemoryIdentifierSystem() {
        this.records = new HashMap<>();
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return this.records.containsKey(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        return this.records.get(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        return this.records.get(pid).getPropertyValue(typeDefinition.getIdentifier());
    }

    @Override
    public String registerPID(Map<String, String> properties) throws IOException {
        PIDRecord newRecord = new PIDRecord();
        newRecord.setPid("tmp/test/" + properties.hashCode());
        Set<Entry<String, String>> entries = properties.entrySet();
        entries.forEach(
            entry -> newRecord.addEntry(entry.getKey(), null, entry.getValue())
        );
        this.records.put(newRecord.getPid(), newRecord);
        return newRecord.getPid();
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = this.queryAllProperties(pid);
        // only return properties listed in the type def
        Set<String> typeProps = typeDefinition.getAllProperties();
        PIDRecord result = new PIDRecord();
        for (String propID : allProps.getPropertyIdentifiers()) {
            if (typeProps.contains(propID)) {
                String[] values = allProps.getPropertyValues(propID);
                for (String value : values) {
                    result.addEntry(propID, "", value);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deletePID(String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }
}
