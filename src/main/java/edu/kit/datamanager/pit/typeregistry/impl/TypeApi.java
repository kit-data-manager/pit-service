package edu.kit.datamanager.pit.typeregistry.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.ImmutableList;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.typeregistry.RegisteredProfile;
import edu.kit.datamanager.pit.typeregistry.RegisteredProfileAttribute;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TypeApi implements ITypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(TypeApi.class);

    protected final URL baseUrl;
    protected final RestClient http;
    protected final AsyncLoadingCache<String, RegisteredProfile> profileCache;
    protected final AsyncLoadingCache<String, AttributeInfo> attributeCache;

    public TypeApi(ApplicationProperties properties) {
        this.baseUrl = properties.getTypeRegistryUri();
        String baseUri = null;
        try {
            baseUri = baseUrl.toURI().resolve("v1/types").toString();
        } catch (URISyntaxException e) {
            throw new InvalidConfigException("Type-Api base url not valid: " + baseUrl);
        }
        this.http = RestClient.builder().baseUrl(baseUri).build();

        // TODO better name caching properties (and consider extending them)
        int maximumSize = properties.getMaximumSize();
        long expireAfterWrite = properties.getExpireAfterWrite();

        this.profileCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .executor(Application.EXECUTOR)
                .refreshAfterWrite(Duration.ofMinutes(expireAfterWrite / 2))
                .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
                .removalListener((key, value, cause) ->
                        LOG.trace("Removing profile {} from profile cache. Cause: {}", key, cause)
                )
                .buildAsync(maybeProfilePid -> {
                    LOG.trace("Loading profile {} to cache.", maybeProfilePid);
                    return this.queryProfile(maybeProfilePid);
                });

        this.attributeCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .executor(Application.EXECUTOR)
                .refreshAfterWrite(Duration.ofMinutes(expireAfterWrite / 2))
                .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
                .removalListener((key, value, cause) ->
                        LOG.trace("Removing profile {} from profile cache. Cause: {}", key, cause)
                )
                .buildAsync(attributePid -> {
                    LOG.trace("Loading profile {} to cache.", attributePid);
                    return this.queryAttribute(attributePid);
                });
    }

    private AttributeInfo queryAttribute(String attributePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .path(attributePid)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        InputStream inputStream = clientResponse.getBody();
                        String body = new String(inputStream.readAllBytes());
                        inputStream.close();
                        return extractAttributeInformation(attributePid, Application.jsonObjectMapper().readTree(body));
                    } else {
                        throw new TypeNotFoundException(attributePid);
                    }
                });
    }

    private AttributeInfo extractAttributeInformation(String attributePid, JsonNode jsonNode) {
        String typeName = jsonNode.path("type").asText();
        String name = jsonNode.path("name").asText();
        Schema schema = this.querySchema(attributePid);
        return new AttributeInfo(attributePid, name, typeName, schema);
    }

    protected Schema querySchema(String maybeSchemaPid) throws TypeNotFoundException, ExternalServiceException {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("schema")
                        .path(maybeSchemaPid)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        InputStream inputStream = clientResponse.getBody();
                        Schema schema;
                        try {
                            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            throw new ExternalServiceException(baseUrl.toString(), "Response (" + maybeSchemaPid + ") is not a valid schema.");
                        } finally {
                            inputStream.close();
                        }
                        return schema;
                    } else {
                        throw new TypeNotFoundException(maybeSchemaPid);
                    }
                });
    }

    protected RegisteredProfile queryProfile(String maybeProfilePid) throws TypeNotFoundException, ExternalServiceException {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .path(maybeProfilePid)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        InputStream inputStream = clientResponse.getBody();
                        String body = new String(inputStream.readAllBytes());
                        inputStream.close();
                        return extractProfileInformation(maybeProfilePid, Application.jsonObjectMapper().readTree(body));
                    } else {
                        throw new TypeNotFoundException(maybeProfilePid);
                    }
                });
    }

    protected RegisteredProfile extractProfileInformation(String profilePid, JsonNode typeApiResponse)
            throws TypeNotFoundException, ExternalServiceException {

        List<RegisteredProfileAttribute> attributes = new ArrayList<>();
        typeApiResponse.path("content").path("properties").forEach(item -> {

            String attributePid = Optional.ofNullable(item.path("pid").asText(null))
                    .or(() -> Optional.ofNullable(item.path("identifier").asText(null)))
                    .or(() -> Optional.ofNullable(item.path("id").asText()))
                    .orElse("");

            JsonNode representations = item.path("representationsAndSemantics").path(0);

            JsonNode obligationNode = representations.path("obligation");
            boolean attributeMandatory = obligationNode.isBoolean() ? obligationNode.asBoolean()
                    : List.of("mandatory", "yes", "true").contains(obligationNode.asText().trim().toLowerCase());

            JsonNode repeatableNode = representations.path("repeatable");
            boolean attributeRepeatable = repeatableNode.isBoolean() ? repeatableNode.asBoolean()
                    : List.of("yes", "true", "repeatable").contains(repeatableNode.asText().trim().toLowerCase());

            RegisteredProfileAttribute attribute = new RegisteredProfileAttribute(
                    attributePid,
                    attributeMandatory,
                    attributeRepeatable);

            if (obligationNode.isNull() || repeatableNode.isNull() || attributePid.trim().isEmpty()) {
                throw new ExternalServiceException(baseUrl.toString(), "Malformed attribute in profile (%s): " + attribute);
            }
            attributes.add(attribute);

        });

        return new RegisteredProfile(profilePid, new ImmutableList<>(attributes));
    }

    @Override
    public CompletableFuture<AttributeInfo> queryAttributeInfo(String attributePid) {
        return this.attributeCache.get(attributePid);
    }

    @Override
    public CompletableFuture<RegisteredProfile> queryAsProfile(String profilePid) {
        return this.profileCache.get(profilePid);
    }

    @Override
    public String getRegistryIdentifier() {
        return baseUrl.toString();
    }
}
