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
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

public class DtrTestSchemaGenerator implements SchemaGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(DtrTestSchemaGenerator.class);

    protected static final String ORIGIN = "dtr-test";
    protected final URI baseUrl;
    protected final RestClient http;
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public DtrTestSchemaGenerator(@NotNull ApplicationProperties props) {
        try {
            this.baseUrl = props.getHandleBaseUri().toURI();
        } catch (URISyntaxException e) {
            throw new InvalidConfigException("BaseUrl not configured properly.");
        }
        HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        this.http = RestClient.builder()
                .baseUrl(this.baseUrl.toString())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .requestInterceptor((request, body, execution) -> {
                    long start = System.currentTimeMillis();
                    ClientHttpResponse response = execution.execute(request,  body);
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
    public SchemaInfo generateSchema(@NotNull String maybeTypePid) {
        return this.http.get().uri(uriBuilder -> uriBuilder.pathSegment(maybeTypePid).build())
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        JsonSchema schema = null;
                        try (InputStream inputStream = response.getBody()) {
                            JsonNode schemaDocument = Application.jsonObjectMapper()
                                    .readTree(inputStream)
                                    .path("validationSchema");
                            schema = this.schemaFactory.getSchema(schemaDocument);
                            if (schema == null || schema.getSchemaNode().isEmpty()) {
                                throw new IOException("Could not create valid schema for %s from %s "
                                        .formatted(maybeTypePid, schemaDocument));
                            }
                        } catch (IOException e) {
                            return new SchemaInfo(
                                    ORIGIN,
                                    schema,
                                    new ExternalServiceException(baseUrl.toString(), "No valid schema found resolving PID " + maybeTypePid, e)
                            );
                        }
                        return new SchemaInfo(ORIGIN, schema, null);
                    } else if (status.value() == 404) {
                        return new SchemaInfo(
                                ORIGIN,
                                null,
                                new TypeNotFoundException(maybeTypePid)
                        );
                    } else {
                        return new SchemaInfo(
                                ORIGIN,
                                null,
                                new ExternalServiceException(
                                        this.baseUrl.toString(),
                                        "Error generating schema: %s - %s".formatted(status.value(), status.toString()))
                        );
                    }
                });
    }
}
