package edu.kit.datamanager.pit.pidsystem.impl;

import java.util.*;
import java.util.stream.Collectors;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PidRecordEntry;
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
    public boolean isIdentifierRegistered(String pid) throws ExternalServiceException {
            return this.records.containsKey(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws PidNotFoundException, ExternalServiceException {
        PIDRecord pidRecord = this.records.get(pid);
        if (pidRecord == null) { throw new PidNotFoundException(pid); }
        return pidRecord;
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws PidNotFoundException, ExternalServiceException {
        PIDRecord pidRecord = this.records.get(pid);
        if (pidRecord == null) { throw new PidNotFoundException(pid); }
        if (!pidRecord.hasProperty(typeDefinition.getIdentifier())) { return null; }
        return pidRecord.getPropertyValue(typeDefinition.getIdentifier());
    }
    
    @Override
    public String registerPidUnchecked(final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        this.records.put(pidRecord.pid(), pidRecord);
        LOG.debug("Registered record with PID: {} (etag: {})", pidRecord.pid(), pidRecord.getEtag());
        return pidRecord.pid();
    }

    @Override
    public boolean updatePID(PIDRecord record) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        if (this.records.containsKey(record.pid())) {
            this.records.put(record.pid(), record);
            return true;
        }
        return false;
    }

    @Override
    public boolean deletePID(String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.records.keySet().stream().filter(pid -> pid.startsWith(PREFIX)).collect(Collectors.toSet());
    }
}
