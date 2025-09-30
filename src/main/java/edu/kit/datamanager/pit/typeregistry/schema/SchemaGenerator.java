package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.common.ExternalServiceException;

public interface SchemaGenerator {
    /**
     * Generates a schema for the given type.
     * @param maybeTypePid the PID for the type to generate a schema for.
     * @return the generated schema.
     */
    SchemaInfo generateSchema(String maybeTypePid) throws ExternalServiceException;
}
