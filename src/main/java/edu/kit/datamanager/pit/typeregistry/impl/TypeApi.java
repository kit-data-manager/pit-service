/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaSetGenerator;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Observed
public class TypeApi implements ITypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(TypeApi.class);

    protected final URL baseUrl;
    protected final RestClient http;
    protected final AsyncLoadingCache<String, RegisteredProfile> profileCache;
    protected final AsyncLoadingCache<String, AttributeInfo> attributeCache;

    protected final SchemaSetGenerator schemaSetGenerator;

    public TypeApi(ApplicationProperties properties, SchemaSetGenerator schemaSetGenerator) {
        this.schemaSetGenerator = schemaSetGenerator;
        this.baseUrl = properties.getTypeRegistryUri();
        String baseUri;
        try {
            baseUri = baseUrl.toURI().resolve("v1/types/").toString();
        } catch (URISyntaxException e) {
            throw new InvalidConfigException("Type-Api base url not valid: " + baseUrl);
        }
        this.http = RestClient.builder()
                .baseUrl(baseUri)
                .requestInterceptor((request, body, execution) -> {
                    long start = System.currentTimeMillis();
                    ClientHttpResponse response = execution.execute(request, body);
                    long timeSpan = System.currentTimeMillis() - start;
                    boolean isLongRequest = timeSpan > Application.LONG_HTTP_REQUEST_THRESHOLD;
                    if (isLongRequest) {
                        LOG.warn("Long http request to {} ({}ms)", request.getURI(), timeSpan);
                    }
                    return response;
                })
                .build();

        int maximumSize = properties.getCacheMaxEntries();
        long expireAfterWrite = properties.getCacheExpireAfterWriteLifetime();

        this.profileCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .executor(Application.newExecutor())
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
                .executor(Application.newExecutor())
                .refreshAfterWrite(Duration.ofMinutes(expireAfterWrite / 2))
                .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
                .removalListener((key, value, cause) ->
                        LOG.trace("Removing profile {} from profile cache. Cause: {}", key, cause)
                )
                .buildAsync(attributePid -> {
                    LOG.trace("Loading attribute {} to cache.", attributePid);
                    return this.queryAttribute(attributePid);
                });
    }

    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "typeregistry_query_attribute", description = "Time taken to query attribute from type registry")
    @Counted(value = "typeregistry_query_attribute_total", description = "Total number of attribute queries")
    protected AttributeInfo queryAttribute(@SpanAttribute String attributePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .path(attributePid)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        try (InputStream inputStream = clientResponse.getBody()) {
                            String body = new String(inputStream.readAllBytes());
                            return extractAttributeInformation(attributePid, Application.jsonObjectMapper().readTree(body));
                        } catch (IOException e) {
                            throw new TypeNotFoundException(attributePid);
                        }
                    } else {
                        throw new TypeNotFoundException(attributePid);
                    }
                });
    }

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typeregistry_extract_attribute", description = "Time taken to extract attribute information from JSON")
    protected AttributeInfo extractAttributeInformation(@SpanAttribute String attributePid, JsonNode jsonNode) {
        String typeName = jsonNode.path("type").asText();
        String name = jsonNode.path("name").asText();
        Set<SchemaInfo> schemas = this.querySchemas(attributePid);
        return new AttributeInfo(attributePid, name, typeName, schemas);
    }

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typeregistry_query_schemas", description = "Time taken to query schemas")
    protected Set<SchemaInfo> querySchemas(@SpanAttribute String maybeSchemaPid) throws TypeNotFoundException, ExternalServiceException {
        return schemaSetGenerator.generateFor(maybeSchemaPid).join();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "typeregistry_query_profile", description = "Time taken to query profile from type registry")
    @Counted(value = "typeregistry_query_profile_total", description = "Total number of profile queries")
    protected RegisteredProfile queryProfile(@SpanAttribute String maybeProfilePid) throws TypeNotFoundException, ExternalServiceException {
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

    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "typeregistry_extract_profile", description = "Time taken to extract profile information from JSON")
    protected RegisteredProfile extractProfileInformation(@SpanAttribute String profilePid, JsonNode typeApiResponse)
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

        boolean additionalAttributesDtrTestStyle = StreamSupport
                .stream(typeApiResponse
                        .path("content")
                        .path("representationsAndSemantics")
                        .spliterator(), true)
                .filter(JsonNode::isObject)
                .filter(node -> node.path("expression").asText("").equals("Format"))
                .map(node -> node.path("subSchemaRelation").asText("").equals("allowAdditionalProperties"))
                .findFirst()
                .orElse(true);
        boolean additionalAttributesEoscStyle = typeApiResponse
                .path("content")
                .path("addProps")
                .asBoolean(true);
        // As the default is true, we assume that additional attributes are disallowed if one indicator is false:
        boolean profileDefinitionAllowsAdditionalAttributes = additionalAttributesDtrTestStyle && additionalAttributesEoscStyle;

        return new RegisteredProfile(profilePid, profileDefinitionAllowsAdditionalAttributes, new ImmutableList<>(attributes));
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "typeregistry_query_attribute_info", description = "Time taken to get attribute info (with cache)")
    @Counted(value = "typeregistry_query_attribute_info_total", description = "Total number of attribute info requests")
    public CompletableFuture<AttributeInfo> queryAttributeInfo(@SpanAttribute String attributePid) {
        return this.attributeCache.get(attributePid);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "typeregistry_query_as_profile", description = "Time taken to get profile (with cache)")
    @Counted(value = "typeregistry_query_as_profile_total", description = "Total number of profile requests")
    public CompletableFuture<RegisteredProfile> queryAsProfile(@SpanAttribute String profilePid) {
        return this.profileCache.get(profilePid);
    }

    @Override
    public String getRegistryIdentifier() {
        return baseUrl.toString();
    }
}
