package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.HandleSystemRESTProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import java.util.Base64;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Concrete adapter of an identifier system that connects to the Handle System
 * through its native REST interface available from HS v8 on.
 *
 */
@Component
@ConditionalOnBean(HandleSystemRESTProperties.class)
public class HandleSystemRESTAdapter implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(HandleSystemRESTAdapter.class);

    public static final boolean UNSAFE_SSL = true;

    private final static class TrustAllX509TrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

    }

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    HandleSystemRESTProperties handleProperties;

    protected String baseUri;
    protected String authInfo;
    //protected Client client;
    protected RestTemplate restTemplate = new RestTemplate();

    protected String generatorPrefix;

    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        LOG.trace("Set up HANDLE_REST implementation.");
        this.generatorPrefix = handleProperties.getGeneratorPrefix();
        this.baseUri = applicationProperties.getHandleBaseUri().toString();//UriBuilder.fromUri(baseURI).path("api").build();
        try {
            this.authInfo = Base64.getEncoder().encodeToString(
                URLEncoder.encode(handleProperties.getHandleUser(), "UTF-8")
                    .concat(":")
                    .concat(URLEncoder.encode(handleProperties.getHandlePassword(), "UTF-8"))
                    .getBytes(StandardCharsets.UTF_8)
            );
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Error while encoding the user name in UTF-8", e);
        }

//    if(UNSAFE_SSL){
//      /* TODO: REMOVE THIS IN PRODUCTION VERSION! */
//      try{
//        SSLContext sslContext;
//        sslContext = SSLContext.getInstance("TLS");
//        sslContext.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new java.security.SecureRandom());
//        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
//          public boolean verify(String string, SSLSession ssls){
//            return true;
//          }
//        });
//        
//        this.client = ClientBuilder.newBuilder().sslContext(sslContext).build();
//      } catch(NoSuchAlgorithmException | KeyManagementException e){
//        throw new IllegalStateException("Could not initialize unsafe SSL constructs", e);
//      }
//    } else{
//      this.client = ClientBuilder.newBuilder().build();
//    }
        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public Optional<String> getPrefix() {
        return Optional.of(this.generatorPrefix);
    }

    @Override
    public boolean isIdentifierRegistered(String pid) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
        return response.getStatusCodeValue() == 200;
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        uriBuilder = uriBuilder.queryParam("type", typeDefinition.getIdentifier());
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

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
            throw new IllegalStateException("PID records with more than one property of same type are not supported yet");
        }
        String value = values.get(0).get("data").get("value").asText();
        return value;

    }

    @Override
    public String registerPidUnchecked(final PIDRecord receivedRecord) throws IOException {
        Map<String, List<PIDRecordEntry>> properties = receivedRecord.getEntries();
        ResponseEntity<String> response;
        String pid = receivedRecord.getPid();
        do {
            // PUT record to HS
            Collection<Map<String, String>> record = new LinkedList<>();
            int idx = 0;
            for (String key : properties.keySet()) {
                idx += 1;
                Map<String, String> handleValue = new HashMap<>();
                handleValue.put("index", "" + idx);
                handleValue.put("type", key);
                handleValue.put(
                    "data",
                    objectMapper.writeValueAsString(
                        properties.get(key)
                            .stream()
                            .map( entry -> entry.getValue() )
                            .collect( Collectors.toList() ))
                );
                record.add(handleValue);
            }
            String jsonText = objectMapper.writeValueAsString(record);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
            uriBuilder = uriBuilder.queryParam("overwrite", false);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + authInfo);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
            response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, requestEntity, String.class);

//      response = individualHandleTarget.resolveTemplate("handle", pid).queryParam("overwrite", false).request(MediaType.APPLICATION_JSON)
//              .header("Authorization", "Basic " + authInfo).put(Entity.json(jsonText));
//      // status 409 is sent in case the Handle already exists
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
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.DELETE, requestEntity, String.class);

        return response.getStatusCodeValue() == 200;
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUri).pathSegment("api", "handles", pid);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + authInfo);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().toUri(), HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCodeValue() != 200) {
            throw new PidNotFoundException(pid);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        PIDRecord result = new PIDRecord();
        for (JsonNode valueNode : root.get("values")) {
            if (!(valueNode.get("data").get("format").asText().equals("string") || valueNode.get("data").get("format").asText().equals("base64") || valueNode
                    .get("data").get("format").asText().equals("hex"))) {
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

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws IOException, InvalidConfigException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolveAllPidsOfPrefix'");
    }
}
