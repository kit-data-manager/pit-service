package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SchemaSetGeneratorTest {

    static ApplicationProperties properties;
    static SchemaSetGenerator generator;

    @BeforeAll
    static void setup() throws Exception {
        properties = new ApplicationProperties();
        properties.setCacheExpireAfterWriteLifetime(10);
        properties.setCacheMaxEntries(1000);
        properties.setTypeRegistryUri(new URI("https://typeapi.lab.pidconsortium.net").toURL());
        properties.setHandleBaseUri(new URI("https://hdl.handle.net").toURL());
        generator = new SchemaSetGenerator(properties);
    }

    private static Stream<Arguments> typeWithExamplesAndCounterexamples() {
        return Stream.of( // typePid, example, counterexample
                // checksum
                Arguments.of(
                        "21.T11148/92e200311a56800b3e47",
                        "{ \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }",
                        "\"c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\""),
                // checksum
                Arguments.of(
                        "21.T11148/92e200311a56800b3e47",
                        "\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\"",
                        "\"not a checksum\""),
                // URI with schema making use of "format" to specify a uri
                Arguments.of("21.T11969/cb371c93c5aa0e62198e", "\"https://example.com\"", "This is not a URI")
        );
    }

    @ParameterizedTest
    @MethodSource("typeWithExamplesAndCounterexamples")
    void testExampleAndCounterexample(String typePid, String example, String counterexample) {
        Set<SchemaInfo> schemaInfos = generator.generateFor(typePid).join();
        AttributeInfo attributeInfo = new AttributeInfo(typePid, "name", "typeName", schemaInfos);
        assertTrue(attributeInfo.validate(example));
        assertFalse(attributeInfo.validate(counterexample));
    }
}