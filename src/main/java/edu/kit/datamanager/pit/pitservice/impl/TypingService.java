package edu.kit.datamanager.pit.pitservice.impl;

import com.google.common.cache.LoadingCache;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InconsistentRecordsException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
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

    protected final LoadingCache<String, TypeDefinition> typeCache;
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
            LoadingCache<String, TypeDefinition> typeCache) {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
        this.typeCache = typeCache;
    }

    @Override
    public void validate(PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException {
        this.defaultStrategy.validate(pidRecord);
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        LOG.trace("Performing isIdentifierRegistered({}).", pid);
        return identifierSystem.isIdentifierRegistered(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        LOG.trace("Performing queryProperty({}, TypeDefinition#{}).", pid, typeDefinition.getIdentifier());
        return identifierSystem.queryProperty(pid, typeDefinition);
    }

    @Override
    public String registerPID(PIDRecord record) throws IOException {
        LOG.trace("Performing registerPID({}).", record);
        return identifierSystem.registerPID(record);
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        LOG.trace("Performing queryByType({}, TypeDefinition#{}).", pid, typeDefinition.getIdentifier());
        return identifierSystem.queryByType(pid, typeDefinition);
    }

    @Override
    public boolean deletePID(String pid) throws IOException {
        LOG.trace("Performing deletePID({}).", pid);
        return identifierSystem.deletePID(pid);
    }

    @Override
    public TypeDefinition describeType(String typeIdentifier) throws IOException {
        LOG.trace("Performing describeType({}).", typeIdentifier);
        try {
            LOG.trace("Querying for type with identifier {}.", typeIdentifier);
            return typeCache.get(typeIdentifier);
        } catch (ExecutionException ex) {
            LOG.error("Failed to query for type with identifier " + typeIdentifier + ".", ex);
            throw new InvalidConfigException("Typing service misconfigured.");
        }
    }

    @Override
    public boolean conformsToType(String pid, String typeIdentifier) throws IOException {
        // resolve type record
        LOG.trace("Performing conformsToType({}, {}).", pid, typeIdentifier);
        TypeDefinition typeDef = null;
        try {
            LOG.trace("Query for type with identifier {}.", typeIdentifier);
            typeDef = typeCache.get(typeIdentifier);
        } catch (ExecutionException ex) {
            throw new InvalidConfigException("Typing service misconfigured.");
        }

        if (typeDef == null) {
            LOG.error("Unable to retrieve type for identifier {}.", typeIdentifier);
            throw new TypeNotFoundException(typeIdentifier);
        }
        // resolve PID
        LOG.trace("Resolving PID {}.", pid);
        PIDRecord pidInfo = identifierSystem.queryAllProperties(pid);
        /*
         * Now go through all mandatory properties of the type and check whether
         * they are in the pid data. Remember: both the keys of the pid data map
         * and the type definition record properties are property identifiers
         * (not names)!
         */
        LOG.trace("Validating {} record properties against type with identifier {}.", pidInfo.getEntries().size(), typeIdentifier);
        for (String prop : typeDef.getAllProperties()) {
            LOG.trace("Checking property {} from type definition.", prop);
            if (!typeDef.isOptional(prop) && !pidInfo.hasProperty(prop)) {
                LOG.error("Property {} is not optional and was not found in record. Record {} is not matching type {}.", prop, pid, typeIdentifier);
                //property 'prop' is missing from type info
                return false;
            } else {
                LOG.trace("Property {} found in record.", prop);
            }
        }
        LOG.trace("All mandatory properties were found in record. Record {} is matching type {}.", pid, typeIdentifier);
        return true;
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        LOG.trace("Performing queryAllProperties({}).", pid);
        PIDRecord record = identifierSystem.queryAllProperties(pid);
        if (record == null) {
            throw new PidNotFoundException(pid);
        }
        return record;
    }

    @Override
    public PIDRecord queryAllProperties(String pid, boolean includePropertyNames)
            throws IOException, InconsistentRecordsException {
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
            LOG.trace("Querying for type with identifier {}.", propertyIdentifier);
            typeDef = typeCache.get(propertyIdentifier);
        } catch (ExecutionException ex) {
            LOG.error("Querying for type with identifier {}.", propertyIdentifier);

            throw new InvalidConfigException("Typing service misconfigured.");
        }

        if (typeDef != null) {
            pidInfo.addEntry(propertyIdentifier, typeDef.getName(), identifierSystem.queryProperty(pid, typeDef));
            return pidInfo;
        }
        return null;
    }

    private void enrichPIDInformationRecord(PIDRecord pidInfo) throws IOException {
        // enrich record by querying type registry for all property definitions
        // to get the property names
        for (String typeIdentifier : pidInfo.getPropertyIdentifiers()) {
            TypeDefinition typeDef;
            try {
                typeDef = typeCache.get(typeIdentifier);// typeRegistry.queryTypeDefinition(typeIdentifier);
            } catch (ExecutionException ex) {
                throw new InvalidConfigException("Typing service misconfigured.");
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
            throws IOException, InconsistentRecordsException {
        TypeDefinition typeDef;
        try {
            typeDef = typeCache.get(typeIdentifier);
        } catch (ExecutionException ex) {
            throw new InvalidConfigException("Typing service misconfigured.");
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

    @Override
    public PIDRecord queryByTypeWithConformance(String pid, String typeIdentifier, boolean includePropertyNames)
            throws IOException, InconsistentRecordsException {
        TypeDefinition typeDef;

        try {
            typeDef = typeCache.get(typeIdentifier);
        } catch (ExecutionException ex) {
            throw new InvalidConfigException("Typing service misconfigured.");
        }

        if (typeDef == null) {
            return null;
        }
        // now query PID record
        PIDRecord result = identifierSystem.queryByType(pid, typeDef);
        if (includePropertyNames) {
            enrichPIDInformationRecord(result);
        }
        result.getMissingMandatoryTypesOf(typeDef);
        return result;
    }

    @Override
    public PIDRecord queryByTypeWithConformance(String pid, List<String> typeIdentifiers,
            boolean includePropertyNames) throws IOException, InconsistentRecordsException {
        if (typeIdentifiers.isEmpty()) {
            return null;
        }
        /*
		 * Query PID record - retrieve all properties, then filter. This is not
		 * the most economical way of doing this, but proper filtering would
		 * require a different additional method.
         */
        PIDRecord pidInfo = identifierSystem.queryAllProperties(pid);
        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        HashSet<String> propertiesInTypes = new HashSet<>();
        for (String typeIdentifier : typeIdentifiers) {
            TypeDefinition typeDef = null;
            try {
                typeDef = typeCache.get(typeIdentifier);
            } catch (ExecutionException ex) {
                throw new InvalidConfigException("Typing service misconfigured.");
            }
            if (typeDef == null) {
                return null;
            }
            propertiesInTypes.addAll(typeDef.getAllProperties());
            pidInfo.getMissingMandatoryTypesOf(typeDef);
        }
        pidInfo.removePropertiesNotListed(propertiesInTypes);
        return pidInfo;
    }

    public ITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public IIdentifierSystem getIdentifierSystem() {
        return identifierSystem;
    }

    @Override
    public boolean updatePID(PIDRecord pidRecord) throws IOException {
        return this.identifierSystem.updatePID(pidRecord);
    }

}
