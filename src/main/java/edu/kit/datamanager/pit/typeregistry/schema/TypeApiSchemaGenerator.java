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

package edu.kit.datamanager.pit.typeregistry.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;


@Observed
public class TypeApiSchemaGenerator implements SchemaGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(TypeApiSchemaGenerator.class);

    protected final URL baseUrl;
    protected final RestClient http;
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public TypeApiSchemaGenerator(@NotNull ApplicationProperties props) {
        this.baseUrl = props.getTypeRegistryUri();
        String baseUri;
        try {
            baseUri = baseUrl.toURI().resolve("v1/types").toString();
        } catch (URISyntaxException e) {
            throw new InvalidConfigException("Type-Api base url not valid: " + baseUrl, e);
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
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "typeapi_schema_generator_generate", description = "Time taken to generate schema from Type API")
    @Counted(value = "typeapi_schema_generator_generate_total", description = "Total number of Type API schema generations")
    public SchemaInfo generateSchema(@SpanAttribute @NotNull String maybeTypePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("schema")
                        .path(maybeTypePid)
                        .build())
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    if (statusCode.is2xxSuccessful()) {
                        JsonSchema schema = null;
                        try (InputStream inputStream = response.getBody()) {
                            JsonNode schemaDocument = Application.jsonObjectMapper()
                                    .readTree(inputStream);
                            schema = schemaFactory.getSchema(schemaDocument);
                            if (schema == null || schema.getSchemaNode().isMissingNode() || schema.getSchemaNode().isTextual()) {
                                throw new IOException("Could not create valid schema for %s from %s "
                                        .formatted(maybeTypePid, schemaDocument));
                            }
                            schema.initializeValidators();
                        } catch (IOException e) {
                            return new SchemaInfo(
                                    this.baseUrl.toString(),
                                    schema,
                                    new ExternalServiceException(
                                            baseUrl.toString(),
                                            "Response (" + maybeTypePid + ") is not a valid schema.")
                            );
                        }
                        return new SchemaInfo(this.baseUrl.toString(), schema, null);
                    } else if (statusCode.value() == 404) {
                        return new SchemaInfo(
                                this.baseUrl.toString(),
                                null,
                                new TypeNotFoundException(maybeTypePid));
                    } else {
                        return new SchemaInfo(
                                this.baseUrl.toString(),
                                null,
                                new ExternalServiceException(
                                        this.baseUrl.toString(),
                                        "Error generating schema: %s - %s".formatted(statusCode.value(), response.getStatusText())));
                    }
                });
    }
}
