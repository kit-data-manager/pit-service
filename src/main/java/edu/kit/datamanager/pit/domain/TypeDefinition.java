/* SPDX-License-Identifier: Apache-2.0 */

package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Data;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a type or profile definition in a data type registry.
 * 
 * @author Thomas Jejkal
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(TypeDefinition.class);

    private String name;
    private String identifier;
    private String description;
    private boolean optional = false;
    private boolean repeatable = false;
    private String expression;
    private String value;
    private Schema jsonSchema;

    private ProvenanceInformation provenance;
    @JsonProperty("properties")
    private Map<String, TypeDefinition> subTypes = new HashMap<>();

    @JsonIgnore
    private TypeDefinition resolvedTypeDefinition;

    @JsonIgnore
    public Set<String> getAllProperties() {
        Set<String> props = new HashSet<>();
        Set<Entry<String, TypeDefinition>> entries = subTypes.entrySet();
        entries.forEach(entry -> props.add(entry.getKey()));

        return props;
    }

    public void setSchema(String schema) {
        if (schema == null) {
            return;
        }

        JSONObject jsonSchema = new JSONObject(schema);
        this.jsonSchema = SchemaLoader.load(jsonSchema);
    }

    /**
     * Takes a value and validates it using this types JSON schema.
     * 
     * @param document the value, usually taken from a PID record to be validated.
     * @return true if the given value is valid accodting to this type.
     */
    public boolean validate(String document) {
        LOG.trace("Performing validate({}).", document);
        if (jsonSchema != null) {
            LOG.trace("Using schema-based validation.");
            Object toValidate = document;
            if (document.startsWith("{")) {
                LOG.trace("Creating JSON object from provided value.");
                toValidate = new JSONObject(document);
            }
            try {
                LOG.trace("Validating provided value using type schema.");
                jsonSchema.validate(toValidate);
                LOG.trace("Validation successful.");
            } catch (ValidationException ex) {
                LOG.error("Validation failed.", ex);
                return false;
            }
        } else {
            LOG.trace("No schema available. Skipping validation.");
        }

        return true;
    }

    public boolean isOptional(String property) {
        return subTypes.get(property).isOptional();
    }

    public void addSubType(TypeDefinition subType) {
        subTypes.put(subType.getIdentifier(), subType);
    }
}
