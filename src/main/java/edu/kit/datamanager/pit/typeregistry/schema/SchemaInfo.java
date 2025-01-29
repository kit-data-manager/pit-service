package edu.kit.datamanager.pit.typeregistry.schema;

import com.networknt.schema.JsonSchema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public record SchemaInfo(
        @NotNull String origin,
        @Nullable JsonSchema schema,
        @Nullable Throwable error
) {
    Optional<Throwable> hasError() {
        return Optional.ofNullable(this.error);
    }

    Optional<JsonSchema> hasSchema() {
        return Optional.ofNullable(this.schema);
    }
}
