package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.common.InconsistentRecordsException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import java.net.URISyntaxException;

/**
 * Core implementation class that offers the combined higher-level services
 * through a type registry and an identifier system.
 *
 */
public class TypingService implements ITypingService {

    protected final IIdentifierSystem identifierSystem;
    protected final ITypeRegistry typeRegistry;

    public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry) throws IOException {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return identifierSystem.isIdentifierRegistered(pid);
    }

    
    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        return identifierSystem.queryProperty(pid, typeDefinition);
    }
    
    @Override
    public String registerPID(Map<String, String> properties) throws IOException {
        return identifierSystem.registerPID(properties);
    }

    @Override
    public PIDRecord queryByType(String pid, edu.kit.datamanager.pit.domain.TypeDefinition typeDefinition) throws IOException {
        return identifierSystem.queryByType(pid, typeDefinition);
    }

    @Override
    public boolean deletePID(String pid) {
        return identifierSystem.deletePID(pid);
    }

    @Override
    public edu.kit.datamanager.pit.domain.TypeDefinition describeType(String typeIdentifier) throws IOException {
        try {
            return typeRegistry.queryTypeDefinition(typeIdentifier);
        } catch (URISyntaxException ex) {
            throw new InvalidConfigException("Typing service misconfigured.");
        }
    }

    @Override
    public boolean conformsToType(String pid, String typeIdentifier) throws IOException {
        // resolve type record
        edu.kit.datamanager.pit.domain.TypeDefinition typeDef = null;
        try {
            typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
        } catch (URISyntaxException ex) {
            throw new InvalidConfigException("Typing service misconfigured.");
        }

        if (typeDef == null) {
            throw new IllegalArgumentException("Unknown type: " + typeIdentifier);
        }
        // resolve PID
        PIDRecord pidInfo = identifierSystem.queryAllProperties(pid);
        /*
     * Now go through all mandatory properties of the type and check whether
     * they are in the pid data. Remember: both the keys of the pid data map
     * and the type definition record properties are property identifiers
     * (not names)!
         */

        for (String prop : typeDef.getAllProperties()) {
            if (!pidInfo.hasProperty(prop)) {
                //property 'prop' is missing from type info
                return false;
            }
        }

        return true;
    }

//    @Override
//    public Object genericResolve(String pid) throws IOException {
//        // ask identifier system whether this is a type registry record
//        boolean istypereg = typeRegistry.isTypeRegistryPID(pid);
//        if (istypereg) {
//            Object obj = null;
//            try {
//                obj = typeRegistry.query(pid);
//            } catch (URISyntaxException ex) {
//                throw new InvalidConfigException("Typing service misconfigured.");
//            }
//
//            if (obj == null) {
//                throw new IOException(
//                        "Conflicting records: Identifier registered in PID system and indicating a registry entry, but not in type registry / registered in an unknown type registry!");
//            }
//            if (obj instanceof PropertyDefinition) {
//                return (PropertyDefinition) obj;
//            } else if (obj instanceof TypeDefinition) {
//                return (TypeDefinition) obj;
//            } else if (obj instanceof ProfileDefinition) {
//                return (ProfileDefinition) obj;
//            } else {
//                throw new IOException("Unknown kind of type registry entry!");
//            }
//        } else {
//            // this is a generic PID record (or unresolvable)
//            return queryAllProperties(pid);
//        }
//    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        return identifierSystem.queryAllProperties(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid, boolean includePropertyNames)
            throws IOException, InconsistentRecordsException {
        PIDRecord pidInfo = identifierSystem.queryAllProperties(pid);
        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        return pidInfo;
    }

    @Override
    public PIDRecord queryProperty(String pid, String propertyIdentifier) throws IOException {
        PIDRecord pidInfo = new PIDRecord();
        // query type registry
        edu.kit.datamanager.pit.domain.TypeDefinition typeDef;
        try {
            typeDef = typeRegistry.queryTypeDefinition(propertyIdentifier);
        } catch (URISyntaxException ex) {
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
            edu.kit.datamanager.pit.domain.TypeDefinition typeDef;
            try {
                typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
            } catch (URISyntaxException ex) {
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
        edu.kit.datamanager.pit.domain.TypeDefinition typeDef;
        try {
            typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
        } catch (URISyntaxException ex) {
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
        edu.kit.datamanager.pit.domain.TypeDefinition typeDef;

        try {
            typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
        } catch (URISyntaxException ex) {
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
        result.checkTypeConformance(typeDef);
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
            edu.kit.datamanager.pit.domain.TypeDefinition typeDef = null;
            try {
                typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
            } catch (URISyntaxException ex) {
                throw new InvalidConfigException("Typing service misconfigured.");
            }
            if (typeDef == null) {
                return null;
            }
            propertiesInTypes.addAll(typeDef.getAllProperties());
            pidInfo.checkTypeConformance(typeDef);
        }
        pidInfo.removePropertiesNotListed(propertiesInTypes);
        return pidInfo;
    }

//    @Override
//    public EntityClass determineEntityClass(String identifier) throws IOException {
//        if (typeRegistry.isTypeRegistryPID(identifier)) {
//            // need to ask type registry about it
//            try {
//                return typeRegistry.determineEntityClass(identifier);
//            } catch (URISyntaxException ex) {
//                throw new InvalidConfigException("Typing service misconfigured.");
//            }
//        } else {
//            return EntityClass.OBJECT;
//        }
//    }
    public ITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public IIdentifierSystem getIdentifierSystem() {
        return identifierSystem;
    }

}
