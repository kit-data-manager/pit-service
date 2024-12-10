package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import jakarta.validation.constraints.NotNull;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

public class TypeApiSchemaGenerator implements SchemaGenerator {

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
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    @Override
    public Optional<Schema> generateSchema(@NotNull String maybeTypePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("schema")
                        .path(maybeTypePid)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        Schema schema;
                        try (InputStream inputStream = clientResponse.getBody()) {
                            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            throw new ExternalServiceException(baseUrl.toString(), "Response (" + maybeTypePid + ") is not a valid schema.");
                        }
                        return Optional.ofNullable(schema);
                    } else {
                        return Optional.empty();
                    }
                });
    }
}
