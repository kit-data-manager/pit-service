package edu.kit.datamanager.pit.typeregistry.schema;

import com.networknt.schema.JsonSchema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record SchemaInfo(
        @NotNull String origin,
        @Nullable JsonSchema schema,
        @Nullable Throwable error
) {}
