package edu.kit.datamanager.pit.typeregistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;

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
    public boolean validate(String value) {
        return this.jsonSchema().stream()
                .map(SchemaInfo::schema)
                .filter(Objects::nonNull)
                .anyMatch(schema -> validate(schema, value));
    }

    private boolean validate(JsonSchema schema, String value) {
        try {
            JsonNode toValidate = Application.jsonObjectMapper().readTree(value);
            Set<ValidationMessage> errors = schema.validate(toValidate, executionContext -> {
                // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
                executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
            });
            return errors.isEmpty();
            // TODO we could catch the validation errors here in order to return them to the user
        } catch (Exception e) {
            return false;
        }
    }
}
