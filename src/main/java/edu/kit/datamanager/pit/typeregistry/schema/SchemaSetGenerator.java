package edu.kit.datamanager.pit.typeregistry.schema;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import org.everit.json.schema.Schema;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SchemaSetGenerator {
    private final Set<SchemaGenerator> GENERATORS;
    private final AsyncLoadingCache<String, Set<Schema>> CACHE;

    public SchemaSetGenerator(ApplicationProperties props) {
        GENERATORS = Set.of(
                new TypeApiSchemaGenerator(props),
                new DtrTestSchemaGenerator(props)
        );

        CACHE = Caffeine.newBuilder()
                .maximumSize(props.getMaximumSize())
                .executor(Application.EXECUTOR)
                .refreshAfterWrite(Duration.ofMinutes(props.getExpireAfterWrite() / 2))
                .expireAfterWrite(props.getExpireAfterWrite(), TimeUnit.MINUTES)
                .buildAsync(attributePid -> GENERATORS.stream()
                        .map(schemaGenerator -> schemaGenerator.generateSchema(attributePid))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet())
                );
    }

    public CompletableFuture<Set<Schema>> generateFor(final String attributePid) {
        return this.CACHE.get(attributePid);
    }
}
