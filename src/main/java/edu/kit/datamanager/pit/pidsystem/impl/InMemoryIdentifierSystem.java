/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kit.datamanager.pit.pidsystem.impl;

import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.PIISpanAttribute;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A simple basis for demonstrations or tests of the service. PIDs will be
 * stored in a HashMap and not stored anywhere else.
 */
@Component
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnExpression(
        "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).IN_MEMORY.name() }"
)
@Observed
public class InMemoryIdentifierSystem implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryIdentifierSystem.class);
    private static final String PREFIX = "sandboxed/";
    private final Map<String, PIDRecord> records = new HashMap<>();

    public InMemoryIdentifierSystem() {
        LOG.warn("Using in-memory identifier system. REGISTERED PIDs ARE NOT STORED PERMANENTLY.");
    }

    @Override
    public Optional<String> getPrefix() {
        return Optional.of(PREFIX);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_is_pid_registered", description = "Time taken to check if PID is registered in memory system")
    @Counted(value = "memory_system_is_pid_registered_total", description = "Total number of PID registration checks")
    public boolean isPidRegistered(@PIISpanAttribute String pid) throws ExternalServiceException {
        return this.records.containsKey(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_query_pid", description = "Time taken to query PID from memory system")
    @Counted(value = "memory_system_query_pid_total", description = "Total number of PID queries")
    public PIDRecord queryPid(@PIISpanAttribute String pid) throws PidNotFoundException, ExternalServiceException {
        PIDRecord pidRecord = this.records.get(pid);
        if (pidRecord == null) {
            throw new PidNotFoundException(pid);
        }
        return pidRecord;
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_register_pid", description = "Time taken to register PID in memory system")
    @Counted(value = "memory_system_register_pid_total", description = "Total number of PID registrations")
    public String registerPidUnchecked(@PIISpanAttribute final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        this.records.put(pidRecord.getPid(), pidRecord);
        LOG.debug("Registered record with PID: {}", pidRecord.getPid());
        return pidRecord.getPid();
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_update_pid", description = "Time taken to update PID in memory system")
    @Counted(value = "memory_system_update_pid_total", description = "Total number of PID updates")
    public boolean updatePid(@PIISpanAttribute PIDRecord record) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        if (this.records.containsKey(record.getPid())) {
            this.records.put(record.getPid(), record);
            return true;
        }
        return false;
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_delete_pid", description = "Time taken to delete PID from memory system")
    @Counted(value = "memory_system_delete_pid_total", description = "Total number of PID deletion attempts")
    public boolean deletePid(@PIISpanAttribute String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "memory_system_resolve_all_pids", description = "Time taken to resolve all PIDs from memory system")
    @Counted(value = "memory_system_resolve_all_pids_total", description = "Total number of resolve all PIDs requests")
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.records.keySet().stream().filter(pid -> pid.startsWith(PREFIX)).collect(Collectors.toSet());
    }
}
