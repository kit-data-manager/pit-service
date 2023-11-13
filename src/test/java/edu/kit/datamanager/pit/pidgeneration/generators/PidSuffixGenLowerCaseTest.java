package edu.kit.datamanager.pit.pidgeneration.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenConstant;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

class PidSuffixGenLowerCaseTest {
    @Test
    void makesLowerCase() {
        PidSuffixGenerator cGen = new PidSuffixGenConstant();
        PidSuffix c = cGen.generate();
        PidSuffixGenerator g = new PidSuffixGenLowerCase(cGen);
        PidSuffix cLow = g.generate();

        assertNotEquals(c.get(), cLow.get());
        assertEquals(c.get().toLowerCase(), cLow.get());
    }

    @Test
    void lowerCasePersists() {
        PidSuffixGenerator cGen = new PidSuffixGenConstant("already-low_123+*#/");
        PidSuffix c = cGen.generate();
        PidSuffixGenerator g = new PidSuffixGenLowerCase(cGen);
        PidSuffix cLow = g.generate();

        assertEquals(c.get(), cLow.get());
        assertEquals(c.get().toLowerCase(), cLow.get());
    }
}
