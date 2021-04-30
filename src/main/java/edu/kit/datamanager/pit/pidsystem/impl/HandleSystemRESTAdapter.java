package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;

import java.util.ArrayList;
import java.util.Base64;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Concrete adapter of an identifier system that connects to the Handle System
 * through its native REST interface available from Handle System v8 on.
 */
public class HandleSystemRESTAdapter implements IIdentifierSystem {
    /** The Handle prefix */
    protected String generatorPrefix;
    /** Handle base URI */
    protected String baseUri;
    /** Base64 encoded credentials (username + password) */
    protected String authInfo;
    
    /** HTTP Client with REST functionality */
    protected RestTemplate restTemplate = new RestTemplate();
    /** JSON (De-)Serializer */
    private ObjectMapper objectMapper = new ObjectMapper();
    /** Official handle API client */
    protected HSAdapter handle;

    public HandleSystemRESTAdapter(ApplicationProperties applicationProperties) {
        super();
        this.generatorPrefix = applicationProperties.getGeneratorPrefix();
        this.baseUri = applicationProperties.getHandleResolverBaseURI().toString();
        try {
            String encodedUserName = URLEncoder.encode(applicationProperties.getHandleUser(), "UTF-8");
            String encodedPassword = URLEncoder.encode(applicationProperties.getHandlePassword(), "UTF-8");
            String encodedCredentials = encodedUserName + ":" + encodedPassword;
            this.authInfo = Base64.getEncoder().encodeToString(encodedCredentials.getBytes());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Error while encoding the user name in UTF-8", e);
        }

        // TODO test if this works fine
        HostnameVerifier sslVerifier = new DefaultHostnameVerifier();

        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(sslVerifier).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        restTemplate = new RestTemplate(requestFactory);


        Path keyPath = applicationProperties.getHandlePrivateKeyPath();
        byte certificateBytes[];
        try {
            certificateBytes = Files.readAllBytes(keyPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read given private key file.", e);
        }

        Optional<Path> cypherPath = applicationProperties.getHandlePrivateKeyCypherPath();
        byte[] cypherBytes = null;
        if (cypherPath.isPresent()) {
            try {
                cypherBytes = Files.readAllBytes(keyPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read given cypher file to decrypt the private key.", e);
            }
        }
        String[] usernameComponents = applicationProperties.getHandleUser().split(":", 2);
        int index = Integer.parseInt(usernameComponents[0]);
        String username = usernameComponents[1];
        try {
            this.handle = HSAdapterFactory.newInstance(username, index, certificateBytes, cypherBytes);
        } catch (NumberFormatException e) {
            throw e;
        } catch (HandleException e) {
            throw new IllegalArgumentException("Handle prefix configuration failed." + e.getMessage(), e);
        }
    }

    @Override
    public boolean isIdentifierRegistered(String pid) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        ResponseEntity<String> response = restTemplate.exchange(
            uriBuilder.build().toUri(),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class
        );
        return response.getStatusCodeValue() == 200;
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        uriBuilder = uriBuilder.queryParam("type", typeDefinition.getIdentifier());
        ResponseEntity<String> response = restTemplate.exchange(
            uriBuilder.build().toUri(),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class
        );

        // extract the Handle value data entry from the json response
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode values = rootNode.get("values");
        if (!values.isArray()) {
            throw new IllegalStateException("Invalid response format: values must be an array");
        }
        if (values.size() == 0) {
            return null;
        }
        if (values.size() > 1) {
            // More than one property stored at this record
            throw new IllegalStateException(
                    "PID records with more than one property of same type are not supported yet");
        }
        String value = values.get(0).get("data").get("value").asText();
        return value;

    }

    protected String generatePIDName() {
        String uuid = UUID.randomUUID().toString();
        return this.generatorPrefix + "/" + uuid;
    }

    @Override
    public String registerPID(PIDRecord received_record) throws IOException {
        Map<String, List<PIDRecordEntry>> properties = received_record.getEntries();
        ResponseEntity<String> response;
        String pid;
        do {
            pid = generatePIDName();

            // PUT record to Handle System
            Collection<Map<String, String>> record = new LinkedList<>();
            int idx = 0;
            for (String key : properties.keySet()) {
                idx += 1;
                Map<String, String> handleValue = new HashMap<>();
                handleValue.put("index", "" + idx);
                handleValue.put("type", key);
                handleValue.put("data",
                    objectMapper.writeValueAsString(
                        properties
                            .get(key)
                            .stream()
                            .map(entry -> entry.getValue())
                            .collect(Collectors.toList())
                    )
                );
                record.add(handleValue);
            }
            // String jsonText = objectMapper.writeValueAsString(record);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
            uriBuilder = uriBuilder.queryParam("overwrite", false);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + authInfo);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
            response = restTemplate.exchange(
                uriBuilder.build().toUri(),
                HttpMethod.GET,
                requestEntity,
                String.class
            );
            // response = individualHandleTarget.resolveTemplate("handle",
            // pid).queryParam("overwrite", false).request(MediaType.APPLICATION_JSON)
            // .header("Authorization", "Basic " + authInfo).put(Entity.json(jsonText));

            // status 409 is sent in case the Handle already exists, so retry with another PID.
        } while (response.getStatusCodeValue() == 409);

        // Evaluate response
        if (response.getStatusCodeValue() == 201) {
            return pid;
        } else {
            throw new IOException("Error trying to create PID " + pid);
        }
    }

    @Override
    public boolean updatePID(PIDRecord record) throws IOException {
        // TODO implement PID update
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = queryAllProperties(pid);
        // only return properties listed in the type def
        Set<String> typeProps = typeDefinition.getAllProperties();
        PIDRecord result = new PIDRecord();
        for (String propID : allProps.getPropertyIdentifiers()) {
            if (typeProps.contains(propID)) {
                String[] values = allProps.getPropertyValues(propID);
                for (String value : values) {
                    result.addEntry(propID, "", value);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deletePID(String pid) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + authInfo);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.DELETE,
                requestEntity, String.class);

        return response.getStatusCodeValue() == 200;
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + authInfo);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET,
                requestEntity, String.class);

        if (response.getStatusCodeValue() != 200) {
            throw new PidNotFoundException(pid);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        PIDRecord result = new PIDRecord();
        for (JsonNode valueNode : root.get("values")) {
            if (!(valueNode.get("data").get("format").asText().equals("string")
                    || valueNode.get("data").get("format").asText().equals("base64")
                    || valueNode.get("data").get("format").asText().equals("hex"))) {
                continue;
            }
            // index is ignored..
            result.addEntry(valueNode.get("type").asText(), "", valueNode.get("data").get("value").asText());
        }
        return result;
    }

    public String getGeneratorPrefix() {
        return generatorPrefix;
    }
}
