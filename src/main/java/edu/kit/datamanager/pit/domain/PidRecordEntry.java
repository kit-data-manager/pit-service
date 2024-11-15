package edu.kit.datamanager.pit.domain;

public record PidRecordEntry(
        String key,
        String name,
        String value
) {
    public PidRecordEntry withName(String name) {
        return new PidRecordEntry(key, name, value);
    }

    public PidRecordEntry withValue(String value) {
        return new PidRecordEntry(key, name, value);
    }
}
