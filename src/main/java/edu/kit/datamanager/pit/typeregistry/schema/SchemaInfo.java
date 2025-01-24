package edu.kit.datamanager.pit.typeregistry.schema;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.everit.json.schema.Schema;

import java.util.Optional;

public record SchemaInfo(
        @NotNull String origin,
        @Nullable Schema schema,
        @Nullable Throwable error
) {
    Optional<Throwable> hasError() {
        return Optional.ofNullable(this.error);
    }

    Optional<Schema> hasSchema() {
        return Optional.ofNullable(this.schema);
    }
}
