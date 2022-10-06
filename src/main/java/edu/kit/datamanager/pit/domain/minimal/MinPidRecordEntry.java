package edu.kit.datamanager.pit.domain.minimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Entity
public class MinPidRecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String key;
    private String value;

    public static MinPidRecordEntry from(PIDRecordEntry other) {
        MinPidRecordEntry self = new MinPidRecordEntry();
        self.key = other.getKey();
        self.value = other.getValue();
        return self;
    }

}
