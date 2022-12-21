package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.transaction.annotation.Transactional;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A very simple representation of a PID record to be stored in databases.
 * 
 * Similar to `KnownPid`, it can be stored in a database, but additionally
 * stores all keys and values, similar to SimplePidRecord. The idea is to use
 * this object for storage only and not have a complex mix of annotations.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Transactional
public class PidDatabaseObject {
    
    @Id
    @Column(name = "pid")
    private String pid;
    
    @ElementCollection
    private List<String> keys = new ArrayList<>();

    @ElementCollection
    private List<String> values = new ArrayList<>();

    /** For hibernate */
    PidDatabaseObject() {}

    /** Protected constructor for testing purposes. */
    protected PidDatabaseObject(String pid, String hiddenIndentifier) {
        this.pid = pid;
        PIDRecordEntry hidden = new PIDRecordEntry();
        hidden.setKey(hiddenIndentifier);
        hidden.setValue(hiddenIndentifier);
        this.appendEntry(hidden);
    }

    public PidDatabaseObject(PIDRecord other) {
        this.pid = other.getPid();
        other
            .getEntries()
            .values()
            .stream()
            .flatMap(List<PIDRecordEntry>::stream)
            .forEach(this::appendEntry);
    }

    public void appendEntry(PIDRecordEntry entry) {
        this.keys.add(entry.getKey());
        this.values.add(entry.getValue());
    }
}
