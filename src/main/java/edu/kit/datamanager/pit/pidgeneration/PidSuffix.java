package edu.kit.datamanager.pit.pidgeneration;

/**
 * A thin wrapper around a suffix string.
 * 
 * The purpose is to indicate that this string is missing the prefix part and is
 * not used as a PID accidentially.
 */
public class PidSuffix {
    private String suffix;

    public PidSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String get() {
        return suffix;
    }
}
