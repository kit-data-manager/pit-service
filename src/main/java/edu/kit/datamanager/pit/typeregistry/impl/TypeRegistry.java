package edu.kit.datamanager.pit.typeregistry.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.ProvenanceInformation;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Accessor for a specific instance of a TypeRegistry. The TypeRegistry is
 * uniquely identified by a baseUrl and an identifierPrefix which all types of
 * this particular registry are using. The prefix also allows to determine,
 * whether a given PID might be a type or property registered at this
 * TypeRegistry.
 */
public class TypeRegistry implements ITypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(TypeRegistry.class);
    protected static final Executor EXECUTOR = Executors.newWorkStealingPool(20);

    @Autowired
    public AsyncLoadingCache<String, TypeDefinition> typeCache;
    @Autowired
    private ApplicationProperties applicationProperties;

    protected RestTemplate restTemplate = new RestTemplate();

    @Override
    public TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException, URISyntaxException {
        LOG.trace("Performing queryTypeDefinition({}).", typeIdentifier);
        String[] segments = typeIdentifier.split("/");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUri(
                        applicationProperties
                                .getHandleBaseUri()
                                .toURI())
                .pathSegment(segments);
        LOG.trace("Querying for type definition at URI {}.", uriBuilder);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        LOG.trace("Constructing type definition from response.");
        return constructTypeDefinition(rootNode);
    }

    /**
     * Helper method to construct a type definition from a JSON response
     * received from the TypeRegistry.
     *
     * @param registryRepresentation The type definition.
     * @return The TypeDefinition as object.
     */
    private TypeDefinition constructTypeDefinition(JsonNode registryRepresentation)
            throws JsonProcessingException, IOException, URISyntaxException {
        // TODO We are doing things too complicated here. Deserialization should be
        // easy.
        // But before we change the domain model to do so, we need a lot of tests to
        // make sure things work as before after the changes.
        LOG.trace("Performing constructTypeDefinition(<rootNode>).");
        final String identifier = registryRepresentation.path("identifier").asText(null);
        if (identifier == null) {
            LOG.error("No 'identifier' property found in entry: {}", registryRepresentation);
            throw new IOException("No 'identifier' attribute found in type definition.");
        }

        LOG.trace("Checking for 'properties' attribute.");
        Map<String, TypeDefinition> properties = new ConcurrentHashMap<>();
        List<CompletableFuture<?>> propertiesHandling = Streams.stream(StreamSupport.stream(
                        registryRepresentation.path("properties").spliterator(), false))
                .filter(property -> property.hasNonNull("name"))
                .filter(property -> property.hasNonNull("identifier"))
                .map(property -> {
                    final String name = property.path("name").asText();
                    final String pid = property.path("identifier").asText();
                    return typeCache.get(pid).thenAcceptAsync(
                            typeDefinition -> {
                                final JsonNode semantics = property.path("representationsAndSemantics").path(0);
                                final String expression = semantics.path("expression").asText(null);
                                typeDefinition.setExpression(expression);
                                final String value = semantics.path("value").asText(null);
                                typeDefinition.setValue(value);
                                final String obligation = semantics.path("obligation").asText("Mandatory");
                                typeDefinition.setOptional("Optional".equalsIgnoreCase(obligation));
                                final String repeatable = semantics.path("repeatable").asText("No");
                                typeDefinition.setRepeatable(!"No".equalsIgnoreCase(repeatable));
                                properties.put(name, typeDefinition);
                            }, EXECUTOR);
                })
                .collect(Collectors.toList());

        TypeDefinition result = new TypeDefinition();
        result.setIdentifier(identifier);
        final String description = registryRepresentation.path("description").asText(null);
        result.setDescription(description);
        final String name = registryRepresentation.path("name").asText(null);
        result.setName(name);
        final String validationSchema = registryRepresentation.path("validationSchema").asText(null);
        result.setSchema(validationSchema);

        if (registryRepresentation.has("provenance")) {
            ProvenanceInformation prov = new ProvenanceInformation();
            JsonNode provNode = registryRepresentation.get("provenance");
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

                if (registryRepresentation.has("identifiedBy")) {
                    identified = entryKV.get("identifiedBy").asText();
                }
                if (registryRepresentation.has("name")) {
                    contributorName = entryKV.get("name").asText();
                }
                if (registryRepresentation.has("details")) {
                    details = entryKV.get("details").asText();
                }
                prov.addContributor(identified, contributorName, details);
            }
            result.setProvenance(prov);
        }

        LOG.trace("Finalizing and returning type definition.");
        CompletableFuture.allOf(propertiesHandling.toArray(new CompletableFuture<?>[0])).join();
        properties.keySet().forEach(pd -> result.addSubType(properties.get(pd)));
        this.typeCache.put(identifier, CompletableFuture.completedFuture(result));
        return result;
    }
}
