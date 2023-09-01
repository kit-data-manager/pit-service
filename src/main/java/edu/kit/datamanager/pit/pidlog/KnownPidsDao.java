package edu.kit.datamanager.pit.pidlog;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Object to access known PIDs from the database.
 * 
 * Method implementation documentation is skipped due to automated
 * implementation via spring data, documented in
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods
 * 
 * as well as the general concept documented in
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.core-concepts
 * 
 * There is a introduction to a more high-level query language (JPQL) at
 * https://www.baeldung.com/spring-data-jpa-query
 */
public interface KnownPidsDao extends JpaRepository<KnownPid, String>, JpaSpecificationExecutor<KnownPid> {
    Optional<KnownPid> findByPid(String pid);

    Collection<KnownPid> findDistinctPidsByCreated(Instant created);

    Collection<KnownPid> findDistinctPidsByModified(Instant modified);

    Collection<KnownPid> findDistinctPidsByCreatedBetween(Instant from, Instant to);

    Collection<KnownPid> findDistinctPidsByModifiedBetween(Instant from, Instant to);

    Page<KnownPid> findDistinctPidsByCreated(Instant created, Pageable pageable);

    Page<KnownPid> findDistinctPidsByModified(Instant modified, Pageable pageable);

    Page<KnownPid> findDistinctPidsByCreatedBetween(Instant from, Instant to, Pageable pageable);

    Page<KnownPid> findDistinctPidsByModifiedBetween(Instant from, Instant to, Pageable pageable);

    long countDistinctPidsByCreatedBetween(Instant from, Instant to);

    long countDistinctPidsByModifiedBetween(Instant from, Instant to);

    @Query("SELECT DISTINCT(k) FROM KnownPid k WHERE ?1 MEMBER OF k.supportedLocations")
    Collection<KnownPid> findBySupportedLocationsContain(String location);

    @Query("SELECT DISTINCT(k) FROM KnownPid k WHERE ?1 MEMBER OF k.supportedTypes")
    Collection<KnownPid> findBySupportedTypesContain(String type);
}
