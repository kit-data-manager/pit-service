package edu.kit.datamanager.pit.pidlog;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Stores information about a known PID so it can be stored in a database.
 * 
 * The use of storing this information is to have a logbook of all created PIDs.
 */
@Entity
public class KnownPid implements Serializable {
    @Id
    @NotBlank(message = "The known PID.")
    private String pid;
    @NotNull(message = "The date the PID was created")
    private Instant created;
    @NotNull(message = "The timestamp of the most recently performed modification.")
    private Instant modified;


    public KnownPid() {}

    public KnownPid(String pid, Instant created, Instant modified) {
        this.pid = pid;
        this.created = created;
        this.modified = modified;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((modified == null) ? 0 : modified.hashCode());
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
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
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "KnownPID [created=" + created + ", modified=" + modified + ", pid=" + pid + "]";
    }
}
