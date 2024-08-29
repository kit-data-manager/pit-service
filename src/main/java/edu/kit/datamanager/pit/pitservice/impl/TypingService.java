package edu.kit.datamanager.pit.pitservice.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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


    protected final AsyncLoadingCache<String, TypeDefinition> typeCache;
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

    public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry,
            AsyncLoadingCache<String, TypeDefinition> typeCache) {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
        this.typeCache = typeCache;
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
    public void validate(PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException {
        this.defaultStrategy.validate(pidRecord);
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws ExternalServiceException {
        LOG.trace("Performing isIdentifierRegistered({}).", pid);
        return identifierSystem.isIdentifierRegistered(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryProperty({}, TypeDefinition#{}).", pid, typeDefinition.getIdentifier());
        return identifierSystem.queryProperty(pid, typeDefinition);
    }

    @Override
    public String registerPidUnchecked(final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        LOG.trace("Performing registerPID({}).", pidRecord);
        return identifierSystem.registerPidUnchecked(pidRecord);
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryByType({}, TypeDefinition#{}).", pid, typeDefinition.getIdentifier());
        return identifierSystem.queryByType(pid, typeDefinition);
    }

    @Override
    public boolean deletePID(String pid) throws ExternalServiceException {
        LOG.trace("Performing deletePID({}).", pid);
        return identifierSystem.deletePID(pid);
    }

    @Override
    public CompletableFuture<TypeDefinition> describeType(String typeIdentifier) throws IOException {
        LOG.trace("Performing describeType({}).", typeIdentifier);
        try {
            LOG.trace(LOG_MSG_QUERY_TYPE, typeIdentifier);
            return typeCache.get(typeIdentifier);
        } catch (RuntimeException ex) {
            LOG.error("Failed to query for type with identifier " + typeIdentifier + ".", ex);
            throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
        }
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryAllProperties({}).", pid);
        PIDRecord pidRecord = identifierSystem.queryAllProperties(pid);
        if (pidRecord == null) {
            throw new PidNotFoundException(pid);
        }
        // ensure the PID is always contained
        pidRecord.setPid(pid);
        return pidRecord;
    }

    @Override
    public PIDRecord queryAllProperties(String pid, boolean includePropertyNames)
            throws IOException {
        LOG.trace("Performing queryAllProperties({}, {}).", pid, includePropertyNames);
        PIDRecord pidInfo = identifierSystem.queryAllProperties(pid);
        LOG.trace("PID record found. {}", (includePropertyNames) ? "Adding property names." : "Returning result.");

        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        return pidInfo;
    }

    @Override
    public PIDRecord queryProperty(String pid, String propertyIdentifier) throws IOException {
        LOG.trace("Performing queryProperty({}, {}).", pid, propertyIdentifier);
        PIDRecord pidInfo = new PIDRecord();
        // query type registry
        TypeDefinition typeDef;
        try {
            LOG.trace(LOG_MSG_QUERY_TYPE, propertyIdentifier);
            typeDef = typeCache.get(propertyIdentifier).get();
        } catch (ExecutionException | InterruptedException ex) {
            LOG.error(LOG_MSG_QUERY_TYPE, propertyIdentifier);
            throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
        }

        if (typeDef != null) {
            pidInfo.addEntry(propertyIdentifier, typeDef.getName(), identifierSystem.queryProperty(pid, typeDef));
            return pidInfo;
        }
        return null;
    }

    private void enrichPIDInformationRecord(PIDRecord pidInfo) {
        // enrich record by querying type registry for all property definitions
        // to get the property names
        for (String typeIdentifier : pidInfo.getPropertyIdentifiers()) {
            TypeDefinition typeDef;
            try {
                typeDef = typeCache.get(typeIdentifier).get();
            } catch (ExecutionException | InterruptedException ex) {
                throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
            }

            if (typeDef != null) {
                pidInfo.setPropertyName(typeIdentifier, typeDef.getName());
            } else {
                pidInfo.setPropertyName(typeIdentifier, typeIdentifier);
            }
        }
    }

    @Override
    public PIDRecord queryByType(String pid, String typeIdentifier, boolean includePropertyNames)
            throws IOException {
        TypeDefinition typeDef;
        try {
            typeDef = typeCache.get(typeIdentifier).get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
        }

        if (typeDef == null) {
            return null;
        }
        // now query PID record and fill in information based on property keys in type definition
        PIDRecord result = identifierSystem.queryByType(pid, typeDef);
        if (includePropertyNames) {
            enrichPIDInformationRecord(result);
        }
        return result;
    }

    public ITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public IIdentifierSystem getIdentifierSystem() {
        return identifierSystem;
    }

    @Override
    public boolean updatePID(PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        return this.identifierSystem.updatePID(pidRecord);
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.identifierSystem.resolveAllPidsOfPrefix();
    }

    public Operations getOperations()  {
        return new Operations(this);
    }

}
