package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import edu.kit.datamanager.pit.pidlog.KnownPid;

/**
 * Object to access PID records from the database.
 * Intended to be used only for sandboxed PIDs.
 */
public interface PidDatabaseObjectDao extends JpaRepository<PidDatabaseObject, String>, JpaSpecificationExecutor<PidDatabaseObject> {
    Optional<PidDatabaseObject> findByPid(String pid);
}
