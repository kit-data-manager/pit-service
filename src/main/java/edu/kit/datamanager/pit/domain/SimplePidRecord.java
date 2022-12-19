package edu.kit.datamanager.pit.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimplePidRecord {

    @JsonIgnore
    public static final String CONTENT_TYPE_PURE = "vnd.datamanager.pid.simple";
    @JsonIgnore
    public static final String CONTENT_TYPE = "application/vnd.datamanager.pid.simple+json";

    private String pid;

    @JsonProperty("record")
    private List<SimplePair> pairs;

    /**
     * Required for (de-)serialization.
     */
    public SimplePidRecord() {}

    /**
     * Converts a given PIDRecord representation into a SimplePidRecord
     * representation.
     * 
     * @param rec a given PID record to convert.
     */
    public SimplePidRecord(PIDRecord rec) {
        this.pid = rec.getPid();
        this.pairs = new ArrayList<>();
        for (Entry<String, List<PIDRecordEntry>> entry : rec.getEntries().entrySet()) {
            String key = entry.getKey();
            for (PIDRecordEntry value : entry.getValue()) {
                SimplePair p = new SimplePair(key, value.getValue());
                this.pairs.add(p);
            }
        }
    }

    public List<SimplePair> getPairs() {
        return pairs;
    }

    public void setPairs(List<SimplePair> pairs) {
        this.pairs = pairs;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
