package edu.kit.datamanager.pit.typeregistry.schema;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaSetGeneratorTest {

    static ApplicationProperties properties;
    static SchemaSetGenerator generator;

    @BeforeAll
    static void setup() throws Exception {
        properties = new ApplicationProperties();
        properties.setExpireAfterWrite(10);
        properties.setMaximumSize(1000);
        properties.setTypeRegistryUri(new URI("https://typeapi.lab.pidconsortium.net").toURL());
        properties.setHandleBaseUri(new URI("https://hdl.handle.net").toURL());
        generator = new SchemaSetGenerator(properties);
    }

    /**
     * @throws ValidationException if a schema fails to validate
     * @throws NoSuchElementException if no schema is found
     */
    @Test
    void testChecksumValidation() throws ValidationException, NoSuchElementException {
        // generated test for this error message: Reason:\nAttribute 21.T11148/92e200311a56800b3e47 has a non-complying value { \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }
        Set<SchemaInfo> schemaInfos = generator.generateFor("21.T11148/92e200311a56800b3e47").join();
        AttributeInfo attributeInfo = new AttributeInfo("21.T11148/92e200311a56800b3e47", "name", "typeName", schemaInfos);
        assertTrue(attributeInfo.validate("{ \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }"));
        // This is currently not supported, but would be nice to have:
        assertFalse(attributeInfo.validate("\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\""));
    }
}