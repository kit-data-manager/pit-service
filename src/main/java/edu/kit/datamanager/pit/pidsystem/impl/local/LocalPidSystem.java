package edu.kit.datamanager.pit.pidsystem.impl.local;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A system that stores PIDs on the local machine, in its configured database.
 * 
 * Purpose: This local system is made for demonstrations, preparations or local
 * tests, but may also be used for other cases where PIDs should not be public
 * (yet).
 * 
 * Note: This system has its own PID string format and we can not guarantee that
 * you'll be able to register your PIDs later in another system with the same
 * format. If you need this feature, feel free to open an issue on GitHub:
 * https://github.com/kit-data-manager/pit-service
 * 
 * Configuration: The database configuration of this service is done via the
 * `spring.datasource.*` properties. There is no configuration that controls a
 * separate database only for this system. Consider the InMemoryIdentifierSystem
 * for this.
 */
@Component
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnExpression(
    "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).LOCAL.name() }"
)
@Transactional
public class LocalPidSystem implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPidSystem.class);
    
    @Autowired
    private PidDatabaseObjectDao db;

    private static final String PREFIX = "sandboxed/";

    public LocalPidSystem() {
        LOG.warn("Using local identifier system to store PIDs. REGISTERED PIDs ARE NOT PERMANENTLY OR PUBLICLY STORED.");
    }

    /**
     * For testing only. Allows to inject the database access object afterwards.
     * 
     * @param db the new DAO.
     */
    protected void setDatabase(PidDatabaseObjectDao db) {
        this.db = db;
    }

    /**
     * For testing purposes.
     */
    protected PidDatabaseObjectDao getDatabase() {
        return this.db;
    }

    @Override
    public Optional<String> getPrefix() {
        return Optional.of(PREFIX);
    }

    @Override
    public boolean isPidRegistered(String pid) throws ExternalServiceException {
        return this.db.existsById(pid);
    }

    @Override
    public PidRecord queryPid(String pid) throws PidNotFoundException, ExternalServiceException {
        Optional<PidDatabaseObject> dbo = this.db.findByPid(pid);
        return new PidRecord(dbo.orElseThrow(() -> new PidNotFoundException(pid)));
    }
    
    @Override
    public String registerPidUnchecked(final PidRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        if (this.db.existsById(pidRecord.getPid())) {
            throw new PidAlreadyExistsException(pidRecord.getPid());
        }
        this.db.save(new PidDatabaseObject(pidRecord));
        LOG.debug("Registered record with PID: {}", pidRecord.getPid());
        return pidRecord.getPid();
    }

    @Override
    public boolean updatePid(PidRecord rec) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        if (this.db.existsById(rec.getPid())) {
            this.db.save(new PidDatabaseObject(rec));
            return true;
        }
        return false;
    }

    @Override
    public boolean deletePid(String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.db.findAll().parallelStream()
                .map(dbo -> dbo.getPid())
                .filter(pid -> pid.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }
}
