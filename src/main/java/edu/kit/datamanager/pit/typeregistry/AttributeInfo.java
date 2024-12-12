package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.Collection;
import java.util.Objects;

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

    private boolean validate(Schema schema, String value) {
        try {
            schema.validate(value);
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }
}
