package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * A simple basis for demonstrations or tests of the service. PIDs will be
 * stored in a HashMap and not stored anywhere else.
 */
@Component
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnExpression(
    "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).IN_MEMORY.name() }"
)
public class InMemoryIdentifierSystem implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryIdentifierSystem.class);
    private static final String PREFIX = "sandboxed/";
    private Map<String, PIDRecord> records = new HashMap<>();

    public InMemoryIdentifierSystem() {
        LOG.warn("Using in-memory identifier system. REGISTERED PIDs ARE NOT STORED PERMANENTLY.");
    }

    @Override
    public Optional<String> getPrefix() {
        return Optional.of(PREFIX);
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return this.records.containsKey(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        PIDRecord record = this.records.get(pid);
        if (record == null) { return null; }
        return record;
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord record = this.records.get(pid);
        if (record == null) { throw new PidNotFoundException(pid); }
        if (!record.hasProperty(typeDefinition.getIdentifier())) { return null; }
        return record.getPropertyValue(typeDefinition.getIdentifier());
    }
    
    @Override
    public String registerPID(PIDRecord record) throws IOException {
        int counter = 0;
        do {
            int hash = record.getEntries().hashCode() + counter;
            record.setPid(PREFIX + hash);
            counter++;
        } while (this.records.containsKey(record.getPid()));
        this.records.put(record.getPid(), record);
        LOG.debug("Registered record with PID: {}", record.getPid());
        return record.getPid();
    }

    @Override
    public boolean updatePID(PIDRecord record) throws IOException {
        if (this.records.containsKey(record.getPid())) {
            this.records.put(record.getPid(), record);
            return true;
        }
        return false;
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = this.queryAllProperties(pid);
        if (allProps == null) {return null;}
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

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws IOException, InvalidConfigException {
        return this.records.keySet().stream().filter(pid -> pid.startsWith(PREFIX)).collect(Collectors.toSet());
    }
}
