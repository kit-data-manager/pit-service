package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import jakarta.validation.constraints.NotNull;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

public class TypeApiSchemaGenerator implements SchemaGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(TypeApiSchemaGenerator.class);

    protected final URL baseUrl;
    protected final RestClient http;

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
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("schema")
                        .path(maybeTypePid)
                        .build())
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    if (statusCode.is2xxSuccessful()) {
                        Schema schema = null;
                        try (InputStream inputStream = response.getBody()) {
                            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            return new SchemaInfo(
                                    this.baseUrl.toString(),
                                    schema,
                                    new ExternalServiceException(baseUrl.toString(), "Response (" + maybeTypePid + ") is not a valid schema.")
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
