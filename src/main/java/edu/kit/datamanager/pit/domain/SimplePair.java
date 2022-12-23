package edu.kit.datamanager.pit.domain;

public class SimplePair {
    private String key;

    private String value;

    /**
     * Required for (de-)serialization.
     */
    SimplePair() {}

    SimplePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
