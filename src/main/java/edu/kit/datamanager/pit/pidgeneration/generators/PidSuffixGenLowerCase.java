package edu.kit.datamanager.pit.pidgeneration.generators;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

/**
 * Generates a PID suffix based on a contained generator and returns the result
 * in lower case.
 */
public class PidSuffixGenLowerCase implements PidSuffixGenerator {

private PidSuffixGenerator generator;

    public PidSuffixGenLowerCase(PidSuffixGenerator generator) {
        this.generator = generator;
    }

    @Override
    public PidSuffix generate() {
        return new PidSuffix(this.generator.generate().get().toLowerCase());
    }

}
