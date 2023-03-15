package edu.kit.datamanager.pit.pidgeneration.generators;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

/**
 * Generates a PID suffix based on a contained generator and returns the result
 * in lower case.
 */
public class PidSuffixGenPrefixed implements PidSuffixGenerator {

    private PidSuffixGenerator generator;
    private String prefix;

    public PidSuffixGenPrefixed(PidSuffixGenerator generator, String prefix) {
        this.generator = generator;
        if (this.prefix.endsWith("/")) {
            this.prefix = prefix;
        } else {
            this.prefix = prefix.concat("/");
        }
    }

    @Override
    public PidSuffix generate() {
        String suffix = this.generator.generate().get().toUpperCase();
        return new PidSuffix(this.prefix.concat(suffix));
    }

}
