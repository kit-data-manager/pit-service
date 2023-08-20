package edu.kit.datamanager.pit.pidlog;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;

/**
 * Stores information about a known PID so it can be stored in a database.
 * 
 * The use of storing this information is to have a logbook of all created PIDs.
 */
@Entity
public class KnownPid implements Serializable {
    @Id
    @org.springframework.data.annotation.Id
    @NotBlank(message = "The known PID.")
    private String pid;
    @NotNull(message = "The date the PID was created")
    private Instant created;
    @NotNull(message = "The timestamp of the most recently performed modification.")
    private Instant modified;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> supportedTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> supportedLocations = new HashSet<>();

    public KnownPid() {}

    public KnownPid(String pid, Instant created, Instant modified) {
        this.pid = pid;
        this.created = created;
        this.modified = modified;
    }

    public KnownPid(PIDRecord pidRecord, Operations pidOperations) throws IOException {
        this.pid = pidRecord.getPid();
        this.created = pidOperations.findDateCreated(pidRecord).orElse(new Date()).toInstant();
        this.modified = pidOperations.findDateModified(pidRecord).orElse(new Date()).toInstant();
        this.supportedTypes = pidOperations.findSupportedTypes(pidRecord);
        this.supportedLocations = pidOperations.findSupportedLocations(pidRecord);
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created.truncatedTo(ChronoUnit.MILLIS);
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified.truncatedTo(ChronoUnit.MILLIS);
    }

    public Set<String> getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(Set<String> supportedTypes) {
        this.supportedTypes = supportedTypes;
    }

    public Set<String> getSupportedLocations() {
        return supportedLocations;
    }

    public void setSupportedLocations(Set<String> supportedLocations) {
        this.supportedLocations = supportedLocations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((modified == null) ? 0 : modified.hashCode());
        result = prime * result + ((supportedTypes == null) ? 0 : supportedTypes.hashCode());
        result = prime * result + ((supportedLocations == null) ? 0 : supportedLocations.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KnownPid other = (KnownPid) obj;
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (modified == null) {
            if (other.modified != null)
                return false;
        } else if (!modified.equals(other.modified))
            return false;
        if (supportedTypes == null) {
            if (other.supportedTypes != null)
                return false;
        } else if (!supportedTypes.equals(other.supportedTypes))
            return false;
        if (supportedLocations == null) {
            if (other.supportedLocations != null)
                return false;
        } else if (!supportedLocations.equals(other.supportedLocations))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "KnownPid [pid=" + pid + ", created=" + created + ", modified=" + modified + ", supportedTypes="
                + supportedTypes + ", supportedLocations=" + supportedLocations + "]";
    }

}
