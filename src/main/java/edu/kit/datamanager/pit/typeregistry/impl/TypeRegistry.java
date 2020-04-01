package edu.kit.datamanager.pit.typeregistry.impl;

import edu.kit.datamanager.pit.domain.old.PropertyDefinition;
import edu.kit.datamanager.pit.domain.old.TypeDefinition;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Accessor for a specific instance of a TypeRegistry. The TypeRegistry is
 * uniquely identified by a baseUrl and an identifierPrefix which all types of
 * this particular registry are using. The prefix also allows to determine,
 * whether a given PID might be a type or property registered at this
 * TypeRegistry.
 */
@Component
public class TypeRegistry implements ITypeRegistry {

    @Autowired
    private ApplicationProperties applicationProperties;

    protected RestTemplate restTemplate = new RestTemplate();

    public TypeRegistry() {
    }

//    @Override
//    public PropertyDefinition queryPropertyDefinition(String propertyIdentifier) throws IOException, URISyntaxException {
//        String[] segments = propertyIdentifier.split("/");
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getTypeRegistryUri().toURI()).pathSegment("objects", segments[0], segments[1]);
//        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode rootNode = mapper.readTree(response.getBody());
//        return constructPropertyDefinition(rootNode);
//    }

//    /**
//     * Helper method to construct a property definition from a JSON response
//     * received from the TypeRegistry.
//     *
//     * @param rootNode The property definition.
//     *
//     * @return The PropertyDefinition as object.
//     */
//    private PropertyDefinition constructPropertyDefinition(JsonNode rootNode) {
//        JsonNode entry = rootNode;
//        String propName = entry.get("name").asText();
//        String valuetype = "";
//        String namespace = "";
//        String description = entry.get("description").asText();
//        boolean isPropDef = false;
//        if (entry.has("representationsAndSemantics")) {
//            for (JsonNode reprsem : entry.get("representationsAndSemantics")) {
//                if (reprsem.get("expression").asText().equalsIgnoreCase("format")
//                        && reprsem.get("value").asText().equalsIgnoreCase("PROPERTY_DEFINITION")) {
//                    isPropDef = true;
//                }
//                if (reprsem.get("expression").asText().equalsIgnoreCase("range")) {
//                    valuetype = reprsem.get("value").asText();
//                }
//                if (reprsem.get("expression").asText().equalsIgnoreCase("namespace")) {
//                    namespace = reprsem.get("value").asText();
//                }
//            }
//        }
//        if (!isPropDef) {
//            // this is not a property record!
//            return null;
//        }
//        return new PropertyDefinition(entry.get("identifier").asText(), propName, valuetype, namespace, description);
//    }

//    @Override
//    public List<PropertyDefinition> queryPropertyDefinitionByName(String propertyName) throws IOException {
//        throw new UnsupportedOperationException("not implemented yet");
//    }

    /**
     * Helper method to construct a type definition from a JSON response
     * received from the TypeRegistry.
     *
     * @param rootNode The type definition.
     *
     * @return The TypeDefinition as object.
     */
    private edu.kit.datamanager.pit.domain.TypeDefinition constructTypeDefinition(JsonNode rootNode) throws JsonProcessingException, IOException, URISyntaxException {
        JsonNode entry = rootNode;
        Map<String, edu.kit.datamanager.pit.domain.TypeDefinition> properties = new HashMap<>();
        if (entry.has("properties")) {
            for (JsonNode entryKV : entry.get("properties")) {
                if (!entryKV.has("name")) {
                    continue;
                }
                String key = entryKV.get("name").asText();
                String value = entryKV.get("identifier").asText();
                if (key.equalsIgnoreCase("property")) {
                    // the value is another PID identifier which needs to be
                    // resolved to human readable format
                    edu.kit.datamanager.pit.domain.TypeDefinition type_def = queryTypeDefinition(value);
                    properties.put(value, type_def);
                }
            }
        }
        String typeUseExpl = entry.get("description").asText();
        String name = entry.get("name").asText();
        String identifier = entry.get("identifier").asText();
        edu.kit.datamanager.pit.domain.TypeDefinition result = new edu.kit.datamanager.pit.domain.TypeDefinition();
        result.setName(name);
        result.setIdentifier(identifier);
        
        //read provenance

        // add properties
        properties.keySet().forEach((pd) -> {
            result.addSubType(properties.get(pd));
        });
        return result;
    }

    @Override
    public edu.kit.datamanager.pit.domain.TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException, URISyntaxException {
        String[] segments = typeIdentifier.split("/");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getTypeRegistryUri().toURI()).pathSegment("objects", segments[0], segments[1]);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        return constructTypeDefinition(rootNode);
    }

//  @Override
//  public ProfileDefinition queryProfileDefinition(String profileIdentifier) throws IOException, URISyntaxException{
//    String[] segments = profileIdentifier.split("/");
//    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getTypeRegistryUri().toURI()).pathSegment("objects", segments[0], segments[1]);
//    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
//    ObjectMapper mapper = new ObjectMapper();
//    JsonNode rootNode = mapper.readTree(response.getBody());
//    return constructProfileDefinition(rootNode);
//  }
//    @Override
//    public void removePropertyDefinition(String propertyIdentifier) throws IOException {
//        throw new UnsupportedOperationException("not implemented yet");
//    }

    @Override
    public Object query(String identifier) throws JsonProcessingException, IOException, URISyntaxException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getHandleBaseUri().toURI()).pathSegment("objects", identifier);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        //rootNode contains type

        return constructTypeDefinition(rootNode);
//    EntityClass entityClass = determineEntityClass(rootNode);
//    if(entityClass == null){
//      return null;
//    }
//    if(entityClass == EntityClass.PROPERTY){
//      return constructPropertyDefinition(rootNode);
//    }
//    if(entityClass == EntityClass.TYPE){
//      return constructTypeDefinition(rootNode);
//    }
//    if(entityClass == EntityClass.PROFILE){
//      return constructProfileDefinition(rootNode);
//    }
//    throw new IllegalStateException("Invalid EntityClass enum value: " + entityClass);
    }

//    private EntityClass determineEntityClass(JsonNode rootNode) {
//        JsonNode entry = rootNode;
//        if (entry.has("representationsAndSemantics")) {
//            for (JsonNode reprsem : entry.get("representationsAndSemantics")) {
//                if (reprsem.get("expression").asText().equalsIgnoreCase("format")) {
//                    String v = reprsem.get("value").asText();
//                    if (v.equalsIgnoreCase("PROPERTY_DEFINITION")) {
//                        return EntityClass.PROPERTY;
//                    }
//                    if (v.equalsIgnoreCase("TYPE_DEFINITION")) {
//                        return EntityClass.TYPE;
//                    }
//                    if (v.equalsIgnoreCase("PROFILE_DEFINITION")) {
//                        return EntityClass.PROFILE;
//                    }
//                    String id = "???";
//                    if (entry.get("identifier") != null) {
//                        id = entry.get("identifier").asText();
//                    }
//                    throw new IllegalStateException("Unknown value for "
//                            + PropertyDefinition.IDENTIFIER_PIT_MARKER_PROPERTY + " in record " + id + ": " + v);
//                }
//            }
//        }
//        return null;
//    }

//    @Override
//    public EntityClass determineEntityClass(String identifier) throws IOException, URISyntaxException {
//        // retrieve full record and analyze marker field
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getHandleBaseUri().toURI()).pathSegment("objects", identifier);
//        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode rootNode = mapper.readTree(response.getBody());
//        return determineEntityClass(rootNode);
//    }

    @Override
    public boolean isTypeRegistryPID(String pid) {
        return pid.startsWith(applicationProperties.getGeneratorPrefix());
    }

    public String getIdentifierPrefix() {
        return applicationProperties.getGeneratorPrefix();
    }

}
