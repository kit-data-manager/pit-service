package edu.kit.datamanager.pit.pidlog;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnownPidsDao extends JpaRepository<KnownPID, String>, JpaSpecificationExecutor<KnownPID> {
    Optional<KnownPID> findByPid(String pid);
    Collection<KnownPID> findDistinctPidsByCreated(Instant created);
    Collection<KnownPID> findDistinctPidsByModified(Instant modified);
    Collection<KnownPID> findDistinctPidsByCreatedBetween(Instant from, Instant to);
    Collection<KnownPID> findDistinctPidsByModifiedBetween(Instant from, Instant to);
    Page<KnownPID> findDistinctPidsByCreated(Instant created, Pageable pageable);
    Page<KnownPID> findDistinctPidsByModified(Instant modified, Pageable pageable);
    Page<KnownPID> findDistinctPidsByCreatedBetween(Instant from, Instant to, Pageable pageable);
    Page<KnownPID> findDistinctPidsByModifiedBetween(Instant from, Instant to, Pageable pageable);
    long countDistinctPidsByCreatedBetween(Instant from, Instant to);
    long countDistinctPidsByModifiedBetween(Instant from, Instant to);
}
