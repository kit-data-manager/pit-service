package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import jakarta.validation.constraints.NotNull;
import org.apache.http.client.HttpClient;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

public class DtrTestSchemaGenerator implements SchemaGenerator {

    protected final URL baseUrl;
    protected final RestClient http;

    public DtrTestSchemaGenerator(@NotNull ApplicationProperties props) {
        this.baseUrl = props.getHandleBaseUri();
        this.http = RestClient.builder()
                .baseUrl(this.baseUrl.toString())
                .build();
    }

    @Override
    public Optional<Schema> generateSchema(@NotNull String maybeTypePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment(maybeTypePid.split("/"))
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        Schema schema;
                        try (InputStream inputStream = clientResponse.getBody()) {
                            JSONObject response = new JSONObject(new JSONTokener(inputStream));
                            JSONObject rawSchema = new JSONObject(new JSONTokener(response.getString("validationSchema")));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            throw new ExternalServiceException(baseUrl.toString(), "No valid schema found resolving PID " + maybeTypePid);
                        }
                        return Optional.ofNullable(schema);
                    } else {
                        return Optional.empty();
                    }
                });
    }
}
