package edu.kit.datamanager.pit.pidgeneration.generators;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

/**
 * Generates a PID suffix based on a contained generator and returns the result
 * in upper case.
 */
public class PidSuffixGenUpperCase implements PidSuffixGenerator {

    private PidSuffixGenerator generator;

    public PidSuffixGenUpperCase(PidSuffixGenerator generator) {
        this.generator = generator;
    }

    @Override
    public PidSuffix generate() {
        return new PidSuffix(this.generator.generate().get().toUpperCase());
    }

}
