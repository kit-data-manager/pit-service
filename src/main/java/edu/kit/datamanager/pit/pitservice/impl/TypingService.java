package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;

import java.util.Collection;
import java.util.Optional;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PidRecord;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Core implementation class that offers the combined higher-level services
 * through a type registry and an identifier system.
 *
 */
public class TypingService implements ITypingService {

    private static final Logger LOG = LoggerFactory.getLogger(TypingService.class);
    private static final String LOG_MSG_TYPING_SERVICE_MISCONFIGURED = "Typing service misconfigured.";
    private static final String LOG_MSG_QUERY_TYPE = "Querying for type with identifier {}.";

    protected final IIdentifierSystem identifierSystem;
    protected final ITypeRegistry typeRegistry;

    /**
     * A validation strategy. Will never be null.
     * 
     * ApplicationProperties::defaultValidationStrategy there is always either a
     * default strategy or a noop strategy assigned. Therefore, autowiring will
     * always work. Assigning null is done to avoid warnings on constructor.
     */
    @Autowired
    protected IValidationStrategy defaultStrategy = null;

    public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry) {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public Optional<String> getPrefix() {
        return this.identifierSystem.getPrefix();
    }

    @Override
    public void setValidationStrategy(IValidationStrategy strategy) {
        this.defaultStrategy = strategy;
    }

    @Override
    public void validate(PidRecord pidRecord)
            throws RecordValidationException, ExternalServiceException {
        this.defaultStrategy.validate(pidRecord);
    }

    @Override
    public boolean isPidRegistered(String pid) throws ExternalServiceException {
        LOG.trace("Performing isIdentifierRegistered({}).", pid);
        return identifierSystem.isPidRegistered(pid);
    }

    @Override
    public String registerPidUnchecked(final PidRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        LOG.trace("Performing registerPID({}).", pidRecord);
        return identifierSystem.registerPidUnchecked(pidRecord);
    }

    @Override
    public boolean deletePid(String pid) throws ExternalServiceException {
        LOG.trace("Performing deletePID({}).", pid);
        return identifierSystem.deletePid(pid);
    }

    @Override
    public PidRecord queryPid(String pid) throws PidNotFoundException, ExternalServiceException {
        return queryPid(pid, false);
    }

    public PidRecord queryPid(String pid, boolean includePropertyNames)
            throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryAllProperties({}, {}).", pid, includePropertyNames);
        PidRecord pidInfo = identifierSystem.queryPid(pid);

        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        return pidInfo;
    }

    private void enrichPIDInformationRecord(PidRecord pidInfo) {
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
    public boolean updatePid(PidRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        return this.identifierSystem.updatePid(pidRecord);
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.identifierSystem.resolveAllPidsOfPrefix();
    }

    public Operations getOperations()  {
        return new Operations(this.typeRegistry, this.identifierSystem);
    }

}
