package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PidDatabaseObjectDao extends JpaRepository<PidDatabaseObject, String>, JpaSpecificationExecutor<PidDatabaseObject> {
    Optional<PidDatabaseObject> findByPid(String pid);
}
