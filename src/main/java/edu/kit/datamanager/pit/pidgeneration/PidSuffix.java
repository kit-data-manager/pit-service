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

    /**
     * Returns the suffix string.
     * 
     * @return the suffix without any prefix.
     */
    public String get() {
        return suffix;
    }

    /**
     * Returns the suffix string with the given prefix prepended.
     * 
     * @param prefix the prefix to prepend.
     * @return the prefix + suffix.
     */
    public String getWithPrefix(String prefix) {
        return prefix + suffix;
    }

    /**
     * Ensures a string is prefixed with the given prefix.
     * 
     * It makes sure the prefix is not added, if the string already starts with the
     * prefix.
     * 
     * @param maybeSuffix the string to prefix.
     * @param prefix      the prefix to add.
     * @return the string with the prefix added.
     */
    public static String asPrefixedChecked(String maybeSuffix, String prefix) {
        if (!maybeSuffix.startsWith(prefix)) {
            return prefix + maybeSuffix;
        } else {
            return maybeSuffix;
        }
    }
}
