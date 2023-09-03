/*
 * Adapted from: https://git.rwth-aachen.de/nfdi4ing/s-3/s-3-3/metadatahub/-/blob/main/src/main/java/edu/kit/metadatahub/doip/rest/RestDoip.java#L39
 * 
 * License: Apache 2.0
 */
package edu.kit.datamanager.pit.web.doip;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import edu.kit.datamanager.pit.domain.SimplePair;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "clientId",
    "attributes"
})
public class DoipRequest {

    @JsonProperty("id")
    private String id;
    @JsonProperty("clientId")
    private String clientId;
    @JsonProperty("targetId")
    private String targetId;
    @JsonProperty("token")
    private String token;
    @JsonProperty("attributes")
    private List<SimplePair> attributes = new ArrayList<>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("targetId")
    public String getTargetId() {
        return targetId;
    }

    @JsonProperty("targetId")
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("attributes")
    public List<SimplePair> getAttributes() {
        return attributes;
    }

    @JsonProperty("attributes")
    public void setAttributes(List<SimplePair> attributes) {
        this.attributes = attributes;
    }
}
