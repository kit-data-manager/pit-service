package edu.kit.datamanager.pit.pidgeneration.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenConstant;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

class PidSuffixGenUpperCaseTest {
    @Test
    void makesUpperCase() {
        PidSuffixGenerator cGen = new PidSuffixGenConstant();
        PidSuffix c = cGen.generate();
        PidSuffixGenerator g = new PidSuffixGenUpperCase(cGen);
        PidSuffix cUp = g.generate();

        assertNotEquals(c.get(), cUp.get());
        assertEquals(c.get().toUpperCase(), cUp.get());
    }

    @Test
    void upperCasePersists() {
        PidSuffixGenerator cGen = new PidSuffixGenConstant("ALREADY-UPPER-CASE_123+*#/");
        PidSuffix c = cGen.generate();
        PidSuffixGenerator g = new PidSuffixGenUpperCase(cGen);
        PidSuffix cUp = g.generate();

        assertEquals(c.get(), cUp.get());
        assertEquals(c.get().toUpperCase(), cUp.get());
    }
}
