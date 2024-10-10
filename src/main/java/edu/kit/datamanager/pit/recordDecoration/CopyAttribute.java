package edu.kit.datamanager.pit.recordDecoration;

import edu.kit.datamanager.pit.domain.PIDRecord;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Copies the value from a source attribute to a target attribute.
 * <p>
 * From the given collection of source attributes, the first available
 * attribute will be considered. If the order matters to you, use an
 * ordered collection type.
 *
 * @param sources the attributes considered a source, ordered in preference.
 * @param target  the target attribute to store the value in
 */
public record CopyAttribute(
        @NotNull Collection<String> sources,
        @NotNull String target
) implements RecordModifier {

    public CopyAttribute {
        Objects.requireNonNull(sources);
        Objects.requireNonNull(target);
    }

    @Override
    public PIDRecord apply(PIDRecord record) {
        this.sources.stream()
                .filter(record::hasProperty)
                .findFirst()
                .ifPresent(source -> record.addEntry(
                        this.target,
                        "",
                        record.getPropertyValue(source)));
        return record;
    }
}
