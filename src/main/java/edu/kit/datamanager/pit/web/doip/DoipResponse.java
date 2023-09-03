package edu.kit.datamanager.pit.web.doip;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

// Serialize everything which is not null
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "status", "attributes", "output" })
public class DoipResponse {

    @JsonIgnore
    private DoipStatus doipStatus;
    
    /**
     * An identifier that indicates the status of the request.
     */
    private String status;
    
    /**
     * Optional array of JSON properties; operation stipulated.
     */
    private List<JsonNode> attributes;
    
    /**
     * arbitrary information returned to the client, depending on the operation. If
     * absent, the output for the operation is all segments that follow this
     * response object. If present the output is the JSON object corresponding to
     * the output property, and there must be no further non-empty segments in the
     * request.
     */
    private JsonNode output;

    DoipResponse(DoipStatus status, List<JsonNode> attributes, JsonNode output) {
        this.doipStatus = status;
        this.status = status.getIdentifier();
        this.attributes = attributes;
        this.output = output;
    }

    DoipResponse(DoipStatus status) {
        this.status = status.getIdentifier();
    }

    @JsonIgnore
    public DoipStatus getDoipStatus() {
        return doipStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.doipStatus = DoipStatus.fromIdentifier(status);
        this.status = status;
    }
    
    public List<JsonNode> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<JsonNode> attributes) {
        this.attributes = attributes;
    }
    
    public JsonNode getOutput() {
        return output;
    }
    
    public void setOutput(JsonNode output) {
        this.output = output;
    }

    @JsonIgnore
    public ResponseEntity<DoipResponse> asResponseEntity() {
        return new ResponseEntity<>(this, this.getDoipStatus().getHttpStatus());
    }
}
