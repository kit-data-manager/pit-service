package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import jakarta.validation.constraints.NotNull;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class DtrTestSchemaGenerator implements SchemaGenerator {

    protected static final String ORIGIN = "dtr-test";
    protected final URI baseUrl;
    protected final HttpClient http;

    public DtrTestSchemaGenerator(@NotNull ApplicationProperties props) {
        try {
            this.baseUrl = props.getHandleBaseUri().toURI();
        } catch (URISyntaxException e) {
            throw new InvalidConfigException("BaseUrl not configured properly.");
        }
        this.http = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }

    @Override
    public SchemaInfo generateSchema(@NotNull String maybeTypePid) {
        HttpGet request = new HttpGet(baseUrl.resolve("/" + maybeTypePid));
        try {
            return http.execute(
                    request,
                    response -> {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode / 100 == 2) {
                            Schema schema = null;
                            try (InputStream inputStream = response.getEntity().getContent()) {
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
                        } else if (statusCode == 404) {
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
                                            "Error generating schema: %s - %s".formatted(statusCode, response.getStatusLine().toString()))
                            );
                        }
                    }
            );
        } catch (IOException e) {
            return new SchemaInfo(
                    ORIGIN,
                    null,
                    new ExternalServiceException(baseUrl.toString(), "Error communicating with service.", e)
            );
        }
    }
}
