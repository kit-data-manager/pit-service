package edu.kit.datamanager.pit.typeregistry.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(locations = "/test/application-test.properties", properties = "pit.pidsystem.implementation = LOCAL")
@ActiveProfiles("test")
public class TypeRegistryTest {

    @Autowired
    TypeRegistry typeRegistry;

    final String profileIdentifier = "21.T11148/b9b76f887845e32d29f7";

    /**
     * See if it does not only cache the sub-types but also the profile itself.
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    void isCachingProfiles() throws IOException, URISyntaxException {
        assertEquals(
                null,
                typeRegistry.typeCache.getIfPresent(profileIdentifier));
        assertEquals(0, typeRegistry.typeCache.size());

        typeRegistry.queryTypeDefinition(profileIdentifier);
        assertNotEquals(
                null,
                typeRegistry.typeCache.getIfPresent(profileIdentifier));
        // A profile definition contains type definitions.
        // The cache therefore should have more than one identifiers in cache.
        assertTrue(typeRegistry.typeCache.size() > 1);
    }
}
