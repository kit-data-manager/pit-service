package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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
//@Table(name = "min_pid_records")
public class MinPidRecord {

    @Id
    @Column(name = "pid")
    private String pid;

    //@CollectionTable(
        //    name = "min_record_entries",
        //    joinColumns = @JoinColumn(name = "pid")
        //)
        //@Column(name = "min_entries")
        //@NotNull(message = "A list of entries.")
        //@OneToMany(
            //    cascade = CascadeType.ALL,
            //    orphanRemoval = true
            //)
    @ElementCollection
    private Map<String, String> entries = new HashMap<>();

    /** For hibernate */
    MinPidRecord() {}

    /** Protected constructor for testing purposes. */
    protected MinPidRecord(String pid, String hiddenIndentifier) {
        this.pid = pid;
        this.entries.put(hiddenIndentifier, hiddenIndentifier);
        //MinPidRecordEntry hidden = new MinPidRecordEntry();
        //hidden.setKey(hiddenIndentifier);
        //hidden.setValue(hiddenIndentifier);
        //this.entries.add(hidden);
    }

    MinPidRecord(PIDRecord other) {
        this.pid = other.getPid();

        List<PIDRecordEntry> tmp = new ArrayList<>();
        other.getEntries().values().stream().forEach(tmp::addAll);
        
        //this.entries = tmp.stream().map(MinPidRecordEntry::from).collect(Collectors.to());

    }
}
