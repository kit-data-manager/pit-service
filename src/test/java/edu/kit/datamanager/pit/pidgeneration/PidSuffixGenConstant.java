package edu.kit.datamanager.pit.pidgeneration;

/**
 * A PID suffic generator for testing purposes: It produces always the same
 * suffix.
 * 
 * Use this generator in tests to provide a predictable input to generators
 * modifying the suffix or have tests independent on a specific type of
 * suffixes. It is only available in test builds.
 */
public class PidSuffixGenConstant implements PidSuffixGenerator {

    private String suffix = "tEsT123";

    public PidSuffixGenConstant() {}

    public PidSuffixGenConstant(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public PidSuffix generate() {
        return new PidSuffix(this.suffix);
    }
    
}
