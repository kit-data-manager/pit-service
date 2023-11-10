package edu.kit.datamanager.pit.pidgeneration.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

class PidSuffixGenUuid4Test {
    @Test
    void generatesUUID() {
        PidSuffixGenerator g = new PidSuffixGenUuid4();
        PidSuffix s = g.generate();
        int uuidBits = 128;
        int uuidHexBytes = uuidBits / 4;
        int uuidDashes = 4;
        assertEquals(uuidHexBytes + uuidDashes, s.get().length());
        assertTrue(s.get().contains("-"));
    }
}
