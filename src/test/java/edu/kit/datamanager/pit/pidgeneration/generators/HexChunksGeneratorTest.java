package edu.kit.datamanager.pit.pidgeneration.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

public class HexChunksGeneratorTest {
    @Test
    void testChunksOfFour() {
        int numChunks = 5;
        PidSuffixGenerator gen = new HexChunksGenerator(numChunks);
        String suffix = gen.generate().get();
        String[] chunks = suffix.split("-");
        
        // Must generate as many chunks as requested
        assertEquals(numChunks, chunks.length);
        for (String chunk : chunks) {
            // Must generate chunks with four characters
            assertEquals(4, chunk.length());
            // Must generate hex-strings
            assertTrue(isHexEncoded(chunk));
        }
    }

    public static boolean isHexEncoded(String input) {
        String hexPattern = "^[0-9A-Fa-f]+$";
        return input.matches(hexPattern);
    }
}
