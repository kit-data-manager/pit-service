package edu.kit.datamanager.pit.typeregistry.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.LoadingCache;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.ProvenanceInformation;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(TypeRegistry.class);
    @Autowired
    public LoadingCache<String, TypeDefinition> typeCache;
    @Autowired
    private ApplicationProperties applicationProperties;

    protected RestTemplate restTemplate = new RestTemplate();

    public TypeRegistry() {
    }

    @Override
    public TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException, URISyntaxException {
        LOG.trace("Performing queryTypeDefinition({}).", typeIdentifier);
        String[] segments = typeIdentifier.split("/");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(applicationProperties.getHandleBaseUri().toURI()).pathSegment(segments);
        LOG.trace("Querying for type definition at URI {}.", uriBuilder.toString());
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        LOG.trace("Constructing type definition from response.");
        return constructTypeDefinition(rootNode);
    }

    /**
     * Helper method to construct a type definition from a JSON response
     * received from the TypeRegistry.
     *
     * @param rootNode The type definition.
     *
     * @return The TypeDefinition as object.
     */
    private TypeDefinition constructTypeDefinition(JsonNode rootNode) throws JsonProcessingException, IOException, URISyntaxException {
        // TODO We are doing things too complicated here. Deserialization should be easy.
        // But before we change the domain model to do so, we need a lot of tests to make sure things work as before after the changes.
        LOG.trace("Performing constructTypeDefinition(<rootNode>).");
        JsonNode entry = rootNode;
        Map<String, TypeDefinition> properties = new HashMap<>();
        LOG.trace("Checking for 'properties' attribute.");
        if (entry.has("properties")) {
            LOG.trace("'properties' attribute found. Transferring properties to type definition.");
            for (JsonNode entryKV : entry.get("properties")) {
                LOG.trace("Checking for 'name' property.");
                if (!entryKV.has("name")) {
                    LOG.trace("No 'name' property found. Skipping property {}.", entryKV);
                    continue;
                }

                String key = entryKV.get("name").asText();

                if (!entryKV.has("identifier")) {
                    LOG.trace("No 'identifier' property found. Skipping property {}.", entryKV);
                    continue;
                }

                String value = entryKV.get("identifier").asText();
                LOG.trace("Creating type definition instance for identifier {}.", value);
                TypeDefinition type_def;

                try {
                    type_def = typeCache.get(value);
                } catch (ExecutionException ex) {
                    LOG.error("Failed to obtain type definition via cache.", ex);
                    throw new IOException("Failed to obtain type definition via cache.", ex);
                }
// queryTypeDefinition(value);

                LOG.trace("Checking for sub-types in 'representationsAndSemantics' property.");
                if (entryKV.has("representationsAndSemantics")) {
                    LOG.trace("'representationsAndSemantics' attribute found. Transferring properties to type definition.");
                    JsonNode semNode = entryKV.get("representationsAndSemantics");
                    semNode = semNode.get(0);
                    LOG.trace("Checking for 'expression' property.");
                    if (semNode.has("expression")) {
                        LOG.trace("Setting 'expression' value {}.", semNode.get("expression").asText());
                        type_def.setExpression(semNode.get("expression").asText());
                    }

                    LOG.trace("Checking for 'value' property.");
                    if (semNode.has("value")) {
                        LOG.trace("Setting 'value' value {}.", semNode.get("value").asText());
                        type_def.setValue(semNode.get("value").asText());
                    }

                    LOG.trace("Checking for 'obligation' property.");
                    if (semNode.has("obligation")) {
                        LOG.trace("Setting 'obligation' value {}.", semNode.get("obligation").asText());
                        String obligation = semNode.get("obligation").asText();
                        type_def.setOptional("Optional".equalsIgnoreCase(obligation));
                    }

                    LOG.trace("Checking for 'repeatable' property.");
                    if (semNode.has("repeatable")) {
                        LOG.trace("Setting 'repeatable' value {}.", semNode.get("repeatable").asText());
                        String repeatable = semNode.get("repeatable").asText();
                        type_def.setRepeatable(!"No".equalsIgnoreCase(repeatable));
                    }
                }
                LOG.trace("Adding new sub-type with key {}.", key);
                properties.put(key, type_def);
            }
        }
        String typeUseExpl = null;
        if (entry.has("description")) {
            typeUseExpl = entry.get("description").asText();
        }
        String name = null;
        if (entry.has("name")) {
            name = entry.get("name").asText();
        }

        if (!entry.has("identifier")) {
            LOG.error("No 'identifier' property found.", entry);
            throw new IOException("No 'identifier' attribute found in type definition.");
        }
        String identifier = entry.get("identifier").asText();

        TypeDefinition result = new TypeDefinition();
        result.setName(name);
        result.setDescription(typeUseExpl);
        result.setIdentifier(identifier);
        LOG.trace("Checking for 'validationSchema' property.");
        if (entry.has("validationSchema")) {
            String validationSchema = entry.get("validationSchema").asText();
            result.setSchema(validationSchema);
        }

        LOG.trace("Checking for 'provenance' property.");
        if (entry.has("provenance")) {
            ProvenanceInformation prov = new ProvenanceInformation();
            JsonNode provNode = entry.get("provenance");
            if (provNode.has("creationDate")) {
                String creationDate = provNode.get("creationDate").asText();
                try {
                    prov.setCreationDate(Date.from(Instant.parse(creationDate)));
                } catch (DateTimeParseException ex) {
                    LOG.error("Failed to parse creationDate from value " + creationDate + ".", ex);
                }
            }
            if (provNode.has("lastModificationDate")) {
                String lastModificationDate = provNode.get("lastModificationDate").asText();
                try {
                    prov.setLastModificationDate(Date.from(Instant.parse(lastModificationDate)));
                } catch (DateTimeParseException ex) {
                    LOG.error("Failed to parse lastModificationDate from value " + lastModificationDate + ".", ex);
                }
            }
            for (JsonNode entryKV : provNode.get("contributors")) {
                String identified = null;
                String contributorName = null;
                String details = null;

                if (entry.has("identifiedBy")) {
                    identified = entryKV.get("identifiedBy").asText();
                }
                if (entry.has("name")) {
                    contributorName = entryKV.get("name").asText();
                }
                if (entry.has("details")) {
                    details = entryKV.get("details").asText();
                }
                prov.addContributor(identified, contributorName, details);
            }
            result.setProvenance(prov);
        }

        LOG.trace("Finalizing and returning type definition.");
        properties.keySet().forEach((pd) -> {
            result.addSubType(properties.get(pd));
        });
        return result;
    }
}
