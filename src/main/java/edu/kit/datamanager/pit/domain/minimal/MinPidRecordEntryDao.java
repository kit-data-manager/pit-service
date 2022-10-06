package edu.kit.datamanager.pit.domain.minimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MinPidRecordEntryDao extends JpaRepository<MinPidRecordEntry, String>, JpaSpecificationExecutor<MinPidRecordEntry> {
    
}
