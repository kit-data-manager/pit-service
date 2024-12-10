package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

public class DtrTestSchemaGenerator implements SchemaGenerator {

    protected final URL baseUrl;
    protected final RestClient http;

    public DtrTestSchemaGenerator(ApplicationProperties props) {
        this.baseUrl = props.getHandleBaseUri();
        this.http = RestClient.builder().baseUrl(this.baseUrl.toString()).build();
    }

    @Override
    public Optional<Schema> generateSchema(String maybeTypePid) {
        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment(maybeTypePid.split("/"))
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        InputStream inputStream = clientResponse.getBody();
                        Schema schema;
                        try {
                            JSONObject response = new JSONObject(new JSONTokener(inputStream));
                            JSONObject rawSchema = new JSONObject(new JSONTokener(response.getString("validationSchema")));
                            schema = SchemaLoader.load(rawSchema);
                        } catch (JSONException e) {
                            throw new ExternalServiceException(baseUrl.toString(), "No valid schema found resolving PID " + maybeTypePid);
                        } finally {
                            inputStream.close();
                        }
                        return Optional.ofNullable(schema);
                    } else {
                        return Optional.empty();
                    }
                });
    }
}
