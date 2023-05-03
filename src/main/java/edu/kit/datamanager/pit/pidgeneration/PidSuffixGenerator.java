package edu.kit.datamanager.pit.pidgeneration;

/**
 * Enables the implementor to generate a PID suffix.
 * 
 * Note that a {@link PidSuffixGenerator} might consume another
 * {@link PidSuffixGenerator} to build a suffix from their ourput.
 */
public interface PidSuffixGenerator {
    /**
     * Generate a PID suffix.
     */
    public PidSuffix generate();
}
