package edu.kit.datamanager.pit.typeregistry.schema;

import org.everit.json.schema.Schema;

import java.util.Optional;

public interface SchemaGenerator {
    /**
     * Generates a schema for the given type.
     * @param pid the PID for the type to generate a schema for.
     * @return the generated schema.
     */
    Optional<Schema> generateSchema(String maybeTypePid);
}
