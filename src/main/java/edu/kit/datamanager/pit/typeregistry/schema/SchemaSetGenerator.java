package edu.kit.datamanager.pit.typeregistry.schema;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SchemaSetGenerator {
    protected final Set<SchemaGenerator> GENERATORS;
    protected final AsyncLoadingCache<String, Set<SchemaInfo>> CACHE;

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
                        .collect(Collectors.toSet())
                );
    }

    /**
     * Will generate a set of possible schemas for a given attribute PID and provide information about origin and success.
     * <p>
     * Note that generation may fail and the schema may be null. In such cases, there will usually be error information
     * available.
     *
     * @param attributePid the PID of the attribute to generate schemas for.
     * @return a set of information about the generated schemas, including the schemas themselves, if generation succeeded.
     */
    public CompletableFuture<Set<SchemaInfo>> generateFor(final String attributePid) {
        return this.CACHE.get(attributePid);
    }
}
