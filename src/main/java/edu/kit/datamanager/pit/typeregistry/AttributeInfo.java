package edu.kit.datamanager.pit.typeregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * @param pid the pid of this attribute
 * @param name a human-readable name, defined in the DTR for this type.
 *            Note this is usually different from the name in a specific profile!
 * @param typeName name of the schema type of this attribute in the DTR,
 *                e.g. "Profile", "InfoType", "Special-Info-Type", ...
 * @param jsonSchema the json schema to validate a value of this attribute
 */
public record AttributeInfo(
        String pid,
        String name,
        String typeName,
        Collection<SchemaInfo> jsonSchema
) {
    private static final Logger log = LoggerFactory.getLogger(AttributeInfo.class);

    public boolean validate(String value) {
        return this.jsonSchema().stream()
                .filter(schemaInfo -> schemaInfo.error() == null)
                .filter(schemaInfo -> schemaInfo.schema() != null)
                .peek(schemaInfo -> log.warn("Found valid schema from {} to validate {} / {}.", schemaInfo.origin(), pid, value))
                .anyMatch(schemaInfo -> this.validate(schemaInfo.schema(), value));
    }

    private boolean validate(JsonSchema schema, String value) {
        try {
            JsonNode toValidate = valueToJsonNode(value);
            Set<ValidationMessage> errors = schema.validate(toValidate, executionContext -> {
                // By default, since Draft 2019-09, the format keyword only generates annotations and not assertions
                executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
            });
            if (!errors.isEmpty()) {
                log.warn("Validation errors for value '{}': {}", value, errors);
            }
            return errors.isEmpty();
        } catch (Exception e) {
            log.error("Exception during validation for value '{}': {}", value, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Converts the given value to a JsonNode.
     *
     * @param value the value to convert
     * @return a JsonNode representation of the value
     */
    public static JsonNode valueToJsonNode(String value) {
        JsonNode toValidate;
        if (value.isBlank()) {
            return new TextNode(value);
        }
        try {
            toValidate = Application.jsonObjectMapper().readTree(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse value '{}' as JSON, treating it as a plain text node.", value);
            toValidate = new TextNode(value);
        }
        return toValidate;
    }
}