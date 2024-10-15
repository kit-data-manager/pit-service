package edu.kit.datamanager.pit.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SimplePidRecord(
        String pid,
        @JsonProperty("record") List<SimplePair> pairs
) {

    @JsonIgnore
    public static final String CONTENT_TYPE_PURE = "vnd.datamanager.pid.simple";
    @JsonIgnore
    public static final String CONTENT_TYPE = "application/vnd.datamanager.pid.simple+json";

    /**
     * Converts a given PIDRecord representation into a SimplePidRecord
     * representation.
     * 
     * @param rec a given PID record to convert.
     */
    public SimplePidRecord(PIDRecord rec) {
        this(
                rec.pid(),
                rec.entries().values().stream()
                        .flatMap(attributes -> attributes.stream()
                                .map(entry ->
                                        new SimplePair(entry.key(), entry.value())))
                        .toList());
    }
}
