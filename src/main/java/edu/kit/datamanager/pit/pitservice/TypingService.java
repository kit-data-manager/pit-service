package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.EntityClass;
import edu.kit.datamanager.pit.InconsistentRecordsException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.typeregistry.PropertyDefinition;
import edu.kit.datamanager.pit.typeregistry.TypeDefinition;
import edu.kit.datamanager.pit.typeregistry.ProfileDefinition;

/**
 * Core implementation class that offers the combined higher-level services
 * through a type registry and an identifier system.
 *
 */
public class TypingService implements ITypingService{

  protected final IIdentifierSystem identifierSystem;
  protected final ITypeRegistry typeRegistry;

  public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry) throws IOException{
    super();
    this.identifierSystem = identifierSystem;
    this.typeRegistry = typeRegistry;
  }

  @Override
  public boolean isIdentifierRegistered(String pid) throws IOException{
    return identifierSystem.isIdentifierRegistered(pid);
  }

  @Override
  public String queryProperty(String pid, PropertyDefinition propertyDefinition) throws IOException{
    return identifierSystem.queryProperty(pid, propertyDefinition);
  }

  @Override
  public String registerPID(Map<String, String> properties) throws IOException{
    return identifierSystem.registerPID(properties);
  }

  @Override
  public PIDInformation queryByType(String pid, TypeDefinition typeDefinition) throws IOException{
    return identifierSystem.queryByType(pid, typeDefinition);
  }

  @Override
  public boolean deletePID(String pid){
    return identifierSystem.deletePID(pid);
  }

  @Override
  public PropertyDefinition describeProperty(String propertyIdentifier) throws IOException{
    return typeRegistry.queryPropertyDefinition(propertyIdentifier);
  }

  @Override
  public TypeDefinition describeType(String typeIdentifier) throws IOException{
    return typeRegistry.queryTypeDefinition(typeIdentifier);
  }

  @Override
  public ProfileDefinition describeProfile(String profileIdentifier) throws IOException{
    return typeRegistry.queryProfileDefinition(profileIdentifier);
  }

  @Override
  public boolean conformsToType(String pid, String typeIdentifier) throws IOException{
    // resolve type record
    TypeDefinition typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
    if(typeDef == null){
      throw new IllegalArgumentException("Unknown type: " + typeIdentifier);
    }
    // resolve PID
    PIDInformation pidInfo = identifierSystem.queryAllProperties(pid);
    /*
		 * Now go through all mandatory properties of the type and check whether
		 * they are in the pid data. Remember: both the keys of the pid data map
		 * and the type definition record properties are property identifiers
		 * (not names)!
     */
    for(String p : typeDef.getProperties()){
      if(!pidInfo.hasProperty(p)){
        return false;
      }
    }
    return true;
  }

  @Override
  public Object genericResolve(String pid) throws IOException{
    // ask identifier system whether this is a type registry record
    boolean istypereg = typeRegistry.isTypeRegistryPID(pid);
    if(istypereg){
      Object obj = typeRegistry.query(pid);
      if(obj == null){
        throw new IOException(
                "Conflicting records: Identifier registered in PID system and indicating a registry entry, but not in type registry / registered in an unknown type registry!");
      }
      if(obj instanceof PropertyDefinition){
        return (PropertyDefinition) obj;
      } else if(obj instanceof TypeDefinition){
        return (TypeDefinition) obj;
      } else if(obj instanceof ProfileDefinition){
        return (ProfileDefinition) obj;
      } else{
        throw new IOException("Unknown kind of type registry entry!");
      }
    } else{
      // this is a generic PID record (or unresolvable)
      return queryAllProperties(pid);
    }
  }

  @Override
  public PIDInformation queryAllProperties(String pid) throws IOException{
    return identifierSystem.queryAllProperties(pid);
  }

  @Override
  public PIDInformation queryAllProperties(String pid, boolean includePropertyNames)
          throws IOException, InconsistentRecordsException{
    PIDInformation pidInfo = identifierSystem.queryAllProperties(pid);
    if(includePropertyNames){
      enrichPIDInformationRecord(pidInfo);
    }
    return pidInfo;
  }

  @Override
  public PIDInformation queryProperty(String pid, String propertyIdentifier) throws IOException{
    PIDInformation pidInfo = new PIDInformation();
    // query type registry
    PropertyDefinition propDef = typeRegistry.queryPropertyDefinition(propertyIdentifier);
    if(propDef != null){
      pidInfo.addProperty(propertyIdentifier, propDef.getName(), identifierSystem.queryProperty(pid, propDef));
      return pidInfo;
    }
    return null;
  }

  private void enrichPIDInformationRecord(PIDInformation pidInfo) throws IOException{
    // enrich record by querying type registry for all property definitions
    // to get the property names
    for(String propertyIdentifier : pidInfo.getPropertyIdentifiers()){
      PropertyDefinition propDef = typeRegistry.queryPropertyDefinition(propertyIdentifier);
      if(propDef != null){
        pidInfo.setPropertyName(propertyIdentifier, propDef.getName());
      } else{
        pidInfo.setPropertyName(propertyIdentifier, propertyIdentifier);
      }
    }
  }

  @Override
  public PIDInformation queryByType(String pid, String typeIdentifier, boolean includePropertyNames)
          throws IOException, InconsistentRecordsException{
    TypeDefinition typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
    if(typeDef == null){
      return null;
    }
    // now query PID record
    PIDInformation result = identifierSystem.queryByType(pid, typeDef);
    if(includePropertyNames){
      enrichPIDInformationRecord(result);
    }
    return result;
  }

  @Override
  public PIDInformation queryByTypeWithConformance(String pid, String typeIdentifier, boolean includePropertyNames)
          throws IOException, InconsistentRecordsException{
    TypeDefinition typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
    if(typeDef == null){
      return null;
    }
    // now query PID record
    PIDInformation result = identifierSystem.queryByType(pid, typeDef);
    if(includePropertyNames){
      enrichPIDInformationRecord(result);
    }
    result.checkTypeConformance(typeDef);
    return result;
  }

  @Override
  public PIDInformation queryByTypeWithConformance(String pid, List<String> typeIdentifiers,
          boolean includePropertyNames) throws IOException, InconsistentRecordsException{
    if(typeIdentifiers.isEmpty()){
      return null;
    }
    /*
		 * Query PID record - retrieve all properties, then filter. This is not
		 * the most economical way of doing this, but proper filtering would
		 * require a different additional method.
     */
    PIDInformation pidInfo = identifierSystem.queryAllProperties(pid);
    if(includePropertyNames){
      enrichPIDInformationRecord(pidInfo);
    }
    HashSet<String> propertiesInTypes = new HashSet<>();
    for(String typeIdentifier : typeIdentifiers){
      TypeDefinition typeDef = typeRegistry.queryTypeDefinition(typeIdentifier);
      if(typeDef == null){
        return null;
      }
      propertiesInTypes.addAll(typeDef.getAllProperties());
      pidInfo.checkTypeConformance(typeDef);
    }
    pidInfo.removePropertiesNotListed(propertiesInTypes);
    return pidInfo;
  }

  @Override
  public EntityClass determineEntityClass(String identifier) throws IOException{
    if(typeRegistry.isTypeRegistryPID(identifier)){
      // need to ask type registry about it
      return typeRegistry.determineEntityClass(identifier);
    } else{
      return EntityClass.OBJECT;
    }
  }

  public ITypeRegistry getTypeRegistry(){
    return typeRegistry;
  }

  public IIdentifierSystem getIdentifierSystem(){
    return identifierSystem;
  }

}
