package edu.kit.datamanager.pit.typeregistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.EntityClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class TypeRegistry implements ITypeRegistry{

  protected String identifierPrefix;
  private String baseUri;
  protected RestTemplate restTemplate = new RestTemplate();

  public TypeRegistry(String baseURI, String identifierPrefix){
    this.identifierPrefix = identifierPrefix.trim();
    this.baseUri = baseURI;
  }

  /**
   * Factory method. Generates a new instance from a properties instance.
   *
   * @param properties
   * @return a new TypeRegistry instance.
   * @throws InvalidConfigException
   */
  public static TypeRegistry configFromProperties(Properties properties) throws InvalidConfigException{
    if(!properties.containsKey("typeregistry.baseURI")){
      throw new InvalidConfigException("Property typeregistry.baseURI missing - check configuration!");
    }
    String baseURI = properties.getProperty("typeregistry.baseURI").trim();
    if(!properties.containsKey("typeregistry.identifierPrefix")){
      throw new InvalidConfigException("Property typeregistry.identifierPrefix missing - check configuration!");
    }
    String identifierPrefix = properties.getProperty("typeregistry.identifierPrefix").trim();
    return new TypeRegistry(baseURI, identifierPrefix);
  }

  @Override
  public PropertyDefinition queryPropertyDefinition(String propertyIdentifier) throws IOException{
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("objects", propertyIdentifier);
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.getBody());
    return constructPropertyDefinition(rootNode);
  }

  private PropertyDefinition constructPropertyDefinition(JsonNode rootNode){
    JsonNode entry = rootNode;
    String propName = entry.get("name").asText();
    String valuetype = "";
    String namespace = "";
    String description = entry.get("description").asText();
    boolean isPropDef = false;
    if(entry.has("representationsAndSemantics")){
      for(JsonNode reprsem : entry.get("representationsAndSemantics")){
        if(reprsem.get("expression").asText().equalsIgnoreCase("format")
                && reprsem.get("value").asText().equalsIgnoreCase("PROPERTY_DEFINITION")){
          isPropDef = true;
        }
        if(reprsem.get("expression").asText().equalsIgnoreCase("range")){
          valuetype = reprsem.get("value").asText();
        }
        if(reprsem.get("expression").asText().equalsIgnoreCase("namespace")){
          namespace = reprsem.get("value").asText();
        }
      }
    }
    if(!isPropDef){
      // this is not a property record!
      return null;
    }
    return new PropertyDefinition(entry.get("identifier").asText(), propName, valuetype, namespace, description);
  }

  @Override
  public List<PropertyDefinition> queryPropertyDefinitionByName(String propertyName) throws IOException{
    throw new UnsupportedOperationException("not implemented yet");
  }

  /*
	 * Method edited by: Quan (Gabriel) Zhou @ Indiana University
   */
  private TypeDefinition constructTypeDefinition(JsonNode rootNode) throws JsonProcessingException, IOException{
    JsonNode entry = rootNode;
    boolean isTypeDef = false;
    if(entry.has("representationsAndSemantics")){
      for(JsonNode reprsem : entry.get("representationsAndSemantics")){
        if(reprsem.get("expression").asText().equalsIgnoreCase("format")
                && reprsem.get("value").asText().equalsIgnoreCase("TYPE_DEFINITION")){
          isTypeDef = true;
        }
      }
    }
    if(!isTypeDef){
      // this is not a type record!
      return null;
    }
    Map<String, PropertyDefinition> properties = new HashMap<>();
    if(entry.has("properties")){
      for(JsonNode entryKV : entry.get("properties")){
        String key = entryKV.get("name").asText();
        String value = entryKV.get("identifier").asText();
        if(key.equalsIgnoreCase("property")){
          // the value is another PID identifier which needs to be
          // resolved to human readable format
          PropertyDefinition prop_def = queryPropertyDefinition(value);
          properties.put(value, prop_def);
        }
      }
    }
    String typeUseExpl = entry.get("description").asText();
    String description = entry.get("name").asText();
    TypeDefinition result = new TypeDefinition(entry.get("identifier").asText(), typeUseExpl, description);
    // add properties
    for(String pd : properties.keySet()){
      result.addProperty(pd, properties.get(pd));
    }
    return result;
  }

  @Override
  public TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException{
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("objects", typeIdentifier);
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.getBody());
    return constructTypeDefinition(rootNode);
  }

  @Override
  public void createTypeDefinition(String typeIdentifier, TypeDefinition typeDefinition){
    throw new UnsupportedOperationException("not implemented yet");
    // TODO: also store PIT.Construct field, PIT.Version
  }

  /*
	 * Method author: Quan (Gabriel) Zhou @ Indiana University
   */
  private ProfileDefinition constructProfileDefinition(JsonNode rootNode)
          throws JsonProcessingException, IOException{
    JsonNode entry = rootNode;
    boolean isProfileDef = false;
    if(entry.has("representationsAndSemantics")){
      for(JsonNode reprsem : entry.get("representationsAndSemantics")){
        if(reprsem.get("expression").asText().equalsIgnoreCase("format")
                && reprsem.get("value").asText().equalsIgnoreCase("PROFILE_DEFINITION")){
          isProfileDef = true;
        }
      }
    }
    if(!isProfileDef){
      // this is not a profile record!
      return null;
    }
    Map<String, TypeDefinition> types = new HashMap<>();
    if(entry.has("properties")){
      for(JsonNode entryKV : entry.get("properties")){
        String key = entryKV.get("name").asText();
        String value = entryKV.get("identifier").asText();
        if(key.equalsIgnoreCase("type")){
          // the value is another PID identifier which needs to be
          // resolved to human readable format
          TypeDefinition type_def = queryTypeDefinition(value);
          types.put(value, type_def);
        }
      }
    }
    String profileUseExpl = entry.get("description").asText();
    String description = entry.get("name").asText();
    ProfileDefinition result = new ProfileDefinition(entry.get("identifier").asText(), profileUseExpl, description);
    // add types
    for(String type : types.keySet()){
      result.addType(type, types.get(type));
    }
    return result;
  }

  @Override
  public ProfileDefinition queryProfileDefinition(String profileIdentifier) throws IOException{
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("objects", profileIdentifier);
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.getBody());
    return constructProfileDefinition(rootNode);
  }

  @Override
  public void createProfileDefinition(String profileIdentifier, ProfileDefinition profileDefinition){
    throw new UnsupportedOperationException("not implemented yet");
    // TODO: also store PIT.Construct field, PIT.Version
  }

  @Override
  public void removePropertyDefinition(String propertyIdentifier) throws IOException{
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Object query(String identifier) throws JsonProcessingException, IOException{
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("objects", identifier);
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.getBody());
    EntityClass entityClass = determineEntityClass(rootNode);
    if(entityClass == null){
      return null;
    }
    if(entityClass == EntityClass.PROPERTY){
      return constructPropertyDefinition(rootNode);
    }
    if(entityClass == EntityClass.TYPE){
      return constructTypeDefinition(rootNode);
    }
    if(entityClass == EntityClass.PROFILE){
      return constructProfileDefinition(rootNode);
    }
    throw new IllegalStateException("Invalid EntityClass enum value: " + entityClass);
  }

  private EntityClass determineEntityClass(JsonNode rootNode){
    JsonNode entry = rootNode;
    if(entry.has("representationsAndSemantics")){
      for(JsonNode reprsem : entry.get("representationsAndSemantics")){
        if(reprsem.get("expression").asText().equalsIgnoreCase("format")){
          String v = reprsem.get("value").asText();
          if(v.equalsIgnoreCase("PROPERTY_DEFINITION")){
            return EntityClass.PROPERTY;
          }
          if(v.equalsIgnoreCase("TYPE_DEFINITION")){
            return EntityClass.TYPE;
          }
          if(v.equalsIgnoreCase("PROFILE_DEFINITION")){
            return EntityClass.PROFILE;
          }
          String id = "???";
          if(entry.get("identifier") != null){
            id = entry.get("identifier").asText();
          }
          throw new IllegalStateException("Unknown value for "
                  + PropertyDefinition.IDENTIFIER_PIT_MARKER_PROPERTY + " in record " + id + ": " + v);
        }
      }
    }
    return null;
  }

  @Override
  public EntityClass determineEntityClass(String identifier) throws IOException{
    // retrieve full record and analyze marker field
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("objects", identifier);
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.getBody());
    return determineEntityClass(rootNode);
  }

  @Override
  public boolean isTypeRegistryPID(String pid){
    return pid.startsWith(identifierPrefix);
  }

  public String getIdentifierPrefix(){
    return identifierPrefix;
  }

}
