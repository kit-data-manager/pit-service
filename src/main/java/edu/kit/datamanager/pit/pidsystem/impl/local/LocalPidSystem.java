package edu.kit.datamanager.pit.pidsystem.impl.local;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
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
    PidDatabaseObjectDao db;

    private static final String PREFIX = "sandboxed/";

    public LocalPidSystem() {
        LOG.warn("Using local identifier system to store PIDs. REGISTERED PIDs ARE NOT PERMANENTLY OR PUBLICLY STORED.");
    }

    /**
     * For testing only. Allows to inject the database access object afterwards.
     * 
     * @param db the new DAO.
     */
    public void setDatabase(PidDatabaseObjectDao db) {
        this.db = db;
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return this.db.existsById(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        Optional<PidDatabaseObject> dbo = this.db.findByPid(pid);
        if (dbo.isEmpty()) { return null; }
        return new PIDRecord(dbo.get());
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        Optional<PidDatabaseObject> dbo = this.db.findByPid(pid);
        if (dbo.isEmpty()) { throw new PidNotFoundException(pid); }
        PIDRecord rec = new PIDRecord(dbo.get());
        if (!rec.hasProperty(typeDefinition.getIdentifier())) { return null; }
        return rec.getPropertyValue(typeDefinition.getIdentifier());
    }
    
    @Override
    public String registerPID(PIDRecord rec) throws IOException {
        int counter = 0;
        do {
            int hash = rec.getEntries().hashCode() + counter;
            rec.setPid(PREFIX + hash);
            counter++;
        } while (this.db.existsById(rec.getPid()));
        this.db.save(new PidDatabaseObject(rec));
        LOG.debug("Registered record with PID: {}", rec.getPid());
        return rec.getPid();
    }

    @Override
    public boolean updatePID(PIDRecord rec) throws IOException {
        if (this.db.existsById(rec.getPid())) {
            this.db.save(new PidDatabaseObject(rec));
            return true;
        }
        return false;
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = this.queryAllProperties(pid);
        if (allProps == null) {return null;}
        // only return properties listed in the type def
        Set<String> typeProps = typeDefinition.getAllProperties();
        PIDRecord result = new PIDRecord();
        for (String propID : allProps.getPropertyIdentifiers()) {
            if (typeProps.contains(propID)) {
                String[] values = allProps.getPropertyValues(propID);
                for (String value : values) {
                    result.addEntry(propID, "", value);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deletePID(String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws IOException, InvalidConfigException {
        return this.db.findAll().parallelStream()
                .map(dbo -> dbo.getPid())
                .filter(pid -> pid.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }
}
