package edu.kit.datamanager.pit.typeregistry.schema;

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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

public class DtrTestSchemaGenerator implements SchemaGenerator {

    protected static final String ORIGIN = "dtr-test";
    protected final URI baseUrl;
    protected final RestClient http;

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
                .build();
    }

    @Override
    public SchemaInfo generateSchema(@NotNull String maybeTypePid) {
        return this.http.get().uri(uriBuilder -> uriBuilder.pathSegment(maybeTypePid).build())
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        Schema schema = null;
                        try (InputStream inputStream = response.getBody()) {
                            JSONObject jsonBody = new JSONObject(new JSONTokener(inputStream));
                            JSONObject rawSchema = new JSONObject(new JSONTokener(jsonBody.getString("validationSchema")));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            return new SchemaInfo(
                                    ORIGIN,
                                    schema,
                                    new ExternalServiceException(baseUrl.toString(), "No valid schema found resolving PID " + maybeTypePid)
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
