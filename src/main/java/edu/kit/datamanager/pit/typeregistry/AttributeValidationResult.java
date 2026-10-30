package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AttributeValidationResult {
    public Map<SchemaInfo, SchemaStatus> allSchemas;

    public AttributeValidationResult(
            Collection<SchemaInfo> allSchemas
    ) {
        this.allSchemas = new HashMap<>();
        for (SchemaInfo schemaInfo : allSchemas) {
            this.allSchemas.put(schemaInfo, SchemaStatus.NOT_INVOKED);
        }
    }

    Optional<String> isAcceptedBy() {
        return allSchemas.keySet().stream()
                .filter(schemaInfo -> allSchemas.get(schemaInfo) == SchemaStatus.ACCEPTED)
                .map(SchemaInfo::origin)
                .findFirst();
    }

    boolean hadSchemas() {
        return !allSchemas.isEmpty();
    }
}
