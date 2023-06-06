package edu.kit.datamanager.pit.pidsystem.impl.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A very simple representation of a PID record, without additional information
 * (like human-readable names) to be stored in databases.
 * 
 * Similar to `KnownPid`, it can be stored in a database, but additionally
 * stores all keys and values.
 */
@EqualsAndHashCode
@Data
@Entity
public class PidDatabaseObject {

    @Id
    @Column(name = "pid")
    private String pid;

    /**
     * About the column definition we use here:
     * 
     * int javax.persistence.Column.length() (Optional) The column length. (Applies
     * only if a string-valued column is used.) Default: 255
     * 
     * The h2 database we use for testing has a CHARACTER VARYING limit of
     * 1_000_000_000
     * (http://h2database.com/html/datatypes.html#character_varying_type).
     * 
     * Postgres does not have a limit according to
     * https://www.postgresql.org/docs/current/datatype-character.html, but we
     * assume it will not use its text datatype for the collection anyway.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 1_000_000_000)
    private Map<String, ArrayList<String>> entries = new HashMap<>();

    /** For hibernate */
    public PidDatabaseObject() {}

    /** Protected constructor for testing purposes. */
    protected PidDatabaseObject(String pid, String hiddenIndentifier) {
        this.pid = pid;
        ArrayList<String> values = new ArrayList<>();
        values.add(hiddenIndentifier);
        this.entries.put(hiddenIndentifier, values);
    }

    public PidDatabaseObject(PIDRecord other) {
        this.pid = other.getPid();

        other
            .getEntries()
            .values()
            .stream()
            .flatMap(List<PIDRecordEntry>::stream)
            .forEach(this::addEntry);
    }

    private void addEntry(PIDRecordEntry entry) {
        String key = entry.getKey();
        String value = entry.getValue();
        ArrayList<String> values = this.entries.getOrDefault(key, new ArrayList<>());
        values.add(value);
        this.entries.put(key, values);
    }
}
