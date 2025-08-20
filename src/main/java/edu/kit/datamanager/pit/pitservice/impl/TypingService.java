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

package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.PIISpanAttribute;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

/**
 * Core implementation class that offers the combined higher-level services
 * through a type registry and an identifier system.
 *
 */
@Observed
public class TypingService implements ITypingService {

    private static final Logger LOG = LoggerFactory.getLogger(TypingService.class);
    private static final String LOG_MSG_TYPING_SERVICE_MISCONFIGURED = "Typing service misconfigured.";
    protected final IIdentifierSystem identifierSystem;
    protected final ITypeRegistry typeRegistry;

    /**
     * A validation strategy. Will never be null.
     * <p>
     * ApplicationProperties::defaultValidationStrategy there is always either a
     * default strategy or a noop strategy assigned. Therefore, autowiring will
     * always work. Assigning null is done to avoid warnings on constructor.
     */
    protected IValidationStrategy defaultStrategy;

    public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry, ApplicationProperties applicationProperties) {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
        this.defaultStrategy = applicationProperties.defaultValidationStrategy(typeRegistry);
    }

    @Override
    public Optional<String> getPrefix() {
        return this.identifierSystem.getPrefix();
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_is_pid_registered", description = "Time taken to check PID registration")
    @Counted(value = "typing_service_is_pid_registered_total", description = "Total number of PID registration checks")
    public boolean isPidRegistered(@PIISpanAttribute String pid) throws ExternalServiceException {
        LOG.trace("Performing isIdentifierRegistered({}).", pid);
        return identifierSystem.isPidRegistered(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_query_pid", description = "Time taken to query PID")
    @Counted(value = "typing_service_query_pid_total", description = "Total number of PID queries")
    public PIDRecord queryPid(@PIISpanAttribute String pid) throws PidNotFoundException, ExternalServiceException {
        return queryPid(pid, false);
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_register_pid", description = "Time taken to register PID")
    @Counted(value = "typing_service_register_pid_total", description = "Total number of PID registrations")
    public String registerPidUnchecked(@PIISpanAttribute final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        LOG.trace("Performing registerPID({}).", pidRecord);
        return identifierSystem.registerPidUnchecked(pidRecord);
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_update_pid", description = "Time taken to update PID record")
    @Counted(value = "typing_service_update_pid_total", description = "Total number of PID updates")
    public boolean updatePid(@PIISpanAttribute PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        return this.identifierSystem.updatePid(pidRecord);
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_delete_pid", description = "Time taken to delete PID")
    @Counted(value = "typing_service_delete_pid_total", description = "Total number of PID deletions")
    public boolean deletePid(@PIISpanAttribute String pid) throws ExternalServiceException {
        LOG.trace("Performing deletePID({}).", pid);
        return identifierSystem.deletePid(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_resolve_all_pids", description = "Time taken to resolve all PIDs of prefix")
    @Counted(value = "typing_service_resolve_all_pids_total", description = "Total number of resolve all PIDs requests")
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.identifierSystem.resolveAllPidsOfPrefix();
    }

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_query_pid_with_names", description = "Time taken to query PID with property names")
    @Counted(value = "typing_service_query_pid_with_names_total", description = "Total number of PID queries with names")
    public PIDRecord queryPid(@PIISpanAttribute String pid, @SpanAttribute boolean includePropertyNames)
            throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryAllProperties({}, {}).", pid, includePropertyNames);
        PIDRecord pidInfo = identifierSystem.queryPid(pid);

        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        return pidInfo;
    }

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_enrich_record", description = "Time taken to enrich PID record with property names")
    private void enrichPIDInformationRecord(@PIISpanAttribute PIDRecord pidInfo) {
        // enrich record by querying type registry for all property definitions
        // to get the property names
        for (String typeIdentifier : pidInfo.getPropertyIdentifiers()) {
            AttributeInfo attributeInfo;
            try {
                attributeInfo = this.typeRegistry.queryAttributeInfo(typeIdentifier).join();
            } catch (CompletionException | CancellationException ex) {
                // TODO convert exceptions like in validation service.
                throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
            }

            if (attributeInfo != null) {
                pidInfo.setPropertyName(typeIdentifier, attributeInfo.name());
            } else {
                pidInfo.setPropertyName(typeIdentifier, typeIdentifier);
            }
        }
    }

    @Override
    public void setValidationStrategy(IValidationStrategy strategy) {
        this.defaultStrategy = strategy;
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_validate", description = "Time taken to validate PID record")
    @Counted(value = "typing_service_validate_total", description = "Total number of validations")
    public void validate(@PIISpanAttribute PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException {
        this.defaultStrategy.validate(pidRecord);
    }

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typing_service_get_operations", description = "Time taken to get operations")
    public Operations getOperations() {
        return new Operations(this.typeRegistry, this.identifierSystem);
    }

}
