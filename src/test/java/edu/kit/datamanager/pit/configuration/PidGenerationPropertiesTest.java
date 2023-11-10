package edu.kit.datamanager.pit.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.configuration.PidGenerationProperties.Case;
import edu.kit.datamanager.pit.configuration.PidGenerationProperties.Mode;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidgeneration.generators.HexChunksGeneratorTest;

class PidGenerationPropertiesTest {

    /**
     * Makes assertions on the default PIDs. If this test breaks, we break the
     * default, which indicates a major version release may be required.
     */
    @Test
    void testDefaultPidGenerator() {
        PidGenerationProperties p = new PidGenerationProperties();
        PidSuffixGenerator gen = p.pidGenerator();
        String suffix = gen.generate().get();
        assertDoesNotThrow(
            () -> UUID.fromString(suffix)
        );
    }

    @Test
    void testHexChunksWithBranding() {
        String branding = "BRANDING--";

        PidGenerationProperties p = new PidGenerationProperties();
        p.setMode(Mode.HEX_CHUNKS);
        p.setBrandingPrefix(Optional.of(branding));
        p.setCasing(Case.UPPER);
        p.setNumChunks(15);

        PidSuffixGenerator gen = p.pidGenerator();
        String suffix = gen.generate().get();
        assertTrue(suffix.startsWith(branding));

        String[] parts = suffix.split("--");
        String identifier = parts[1];
        String[] chunks = identifier.split("-");
        
        assertEquals(identifier, identifier.toUpperCase());
        assertEquals(15, chunks.length);
        for (String chunk : chunks) {
            assertTrue(HexChunksGeneratorTest.isHexEncoded(chunk));
        }
    }
}
