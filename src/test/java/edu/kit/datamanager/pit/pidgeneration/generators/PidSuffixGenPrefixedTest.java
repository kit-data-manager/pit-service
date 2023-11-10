package edu.kit.datamanager.pit.pidgeneration.generators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenConstant;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

class PidSuffixGenPrefixedTest {
    @Test
    void makesPrefixes() {
        PidSuffixGenerator c = new PidSuffixGenConstant();
        final String prefix = "my-prefix-123#+";
        assertFalse(c.generate().get().startsWith(prefix));

        PidSuffixGenerator g = new PidSuffixGenPrefixed(c, prefix);
        assertTrue(g.generate().get().startsWith(prefix));
    }
}
