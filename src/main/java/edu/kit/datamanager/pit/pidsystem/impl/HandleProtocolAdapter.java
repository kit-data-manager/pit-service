package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.HandleCredentials;
import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;

/**
 * Uses the official java library to interact with the handle system using the handle protocol.
 */
@Component
@ConditionalOnBean(HandleProtocolProperties.class)
public class HandleProtocolAdapter implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(HandleProtocolProperties.class);

    @Autowired
    private HandleProtocolProperties props;

    private HSAdapter client;

    private boolean isAdminMode = false;

    public HandleProtocolAdapter(HandleProtocolProperties props) {
        this.props = props;
    }

    /**
     * Initializes internal classes.
     * We use this methos with the @PostConstruct annotation to run it
     * after the constructor and after springs autowiring is done properly
     * to make sure that all properties are already autowired.
     * @throws HandleException if a handle system error occurs.
     * @throws InvalidConfigException if the configuration is invalid, e.g. a path does not lead to a file.
     * @throws IOException if the private key file can not be read.
     */
    @PostConstruct
    public void init() throws InvalidConfigException, HandleException, IOException {
        LOG.info("Using PID System 'Handle'");
        this.isAdminMode = props.getCredentials() != null;
        
        if (!this.isAdminMode) {
            LOG.warn("No credentials found. Starting Handle Adapter with no administrative privileges.");
            this.client = HSAdapterFactory.newInstance();

        } else {
            HandleCredentials credentials = props.getCredentials();
            // Check if key file is plausible, throw exceptions if something is wrong.
            {
                File maybeKeyFile = credentials.getPrivateKeyPath().toFile();
                if (!maybeKeyFile.exists()) {
                    throw new InvalidConfigException("PrivateKeyFilePath does not lead to a file.");
                }
                if (!maybeKeyFile.isFile()) {
                    throw new InvalidConfigException("File to private key not a regular file.");
                }
            }
            // NOTE We can still fail later if the private key file contains garbage.

            // Extract information and start handle client with available authentication.
            byte[] privateKey = Files.readAllBytes(credentials.getPrivateKeyPath());
            // Passphrase may be null if key is not encrypted, this is fine.
            byte[] passphrase = null;
            {
                String given = credentials.getPrivateKeyPassphrase();
                if (given != null && !given.isEmpty()) {
                    passphrase = given.getBytes(StandardCharsets.UTF_8);
                    if (passphrase.length < 1) {
                        throw new InvalidConfigException("Passphrase for key file is set but empty!");
                    }
                }
            }
            this.client = HSAdapterFactory.newInstance(
                credentials.getUserHandle(),
                credentials.getPrivateKeyIndex(),
                privateKey,
                passphrase  // "use null for unencrypted keys"
            );
        }
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        HandleValue[] record_properties = null;
        try {
            record_properties = this.client.resolveHandle(pid, null, null);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new IOException(e);
            }
        }
        return record_properties != null && record_properties.length > 0;
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        HandleValue[] record_properties;
        try {
            record_properties = this.client.resolveHandle(pid, null, null);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return null;
            } else {
                throw new IOException(e);
            }
        }
        return this.pidRecordFrom(record_properties).withPID(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        String[] typeArray = {typeDefinition.getIdentifier()};
        try {
            // TODO we assume here that the property only exists once, which will not be true in every case.
            return this.client.resolveHandle(pid, typeArray, null)[0].getDataAsString();
        } catch (HandleException e) {
            if (e.getCode() == HandleException.INVALID_VALUE) {
                return null;
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public String registerPID(PIDRecord record) throws IOException {
        HandleValue[] values = this.handleValuesFrom(record);
        boolean success = false;
        do {
            record.setPid(generateRandomPID());
            try {
                this.client.createHandle(record.getPid(), values);
                success = true;
            } catch (HandleException e) {
                if (e.getCode() != HandleException.HANDLE_ALREADY_EXISTS) {
                    // We already have the loop to handle "HANDLE_ALREAD_EXISTS".
                    // On other errors, we throw an exception.
                    throw new IOException(e);
                }
            }
        } while (!success);
        return record.getPid();
    }

    @Override
    public boolean updatePID(PIDRecord record) throws IOException {
        // TODO: "Make sure that the index value is specified in the array of handle values or else this method will not work well."
        if (!this.isValidPID(record.getPid())) { return false; }
        try {
            this.client.updateHandleValues(record.getPid(), handleValuesFrom(record));
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new IOException(e);
            }
        }
        return true;
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = queryAllProperties(pid);
        // only return properties listed in the type definition
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
    public boolean deletePID(String pid) throws IOException {
        try {
            this.client.deleteHandle(pid);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new IOException(e);
            }
        }
        return false;
    }

    /**
     * Avoids an extra constructor in `PIDRecord`. Instead,
     * keep such details stored in the PID service implementation.
     * @param values HandleValue array that shall be converted into a PIDRecord.
     * @return a PID record with values copied from values.
     */
    protected PIDRecord pidRecordFrom(HandleValue[] values) {
        PIDRecord result = new PIDRecord();
        for (HandleValue v : values) {
            result.addEntry(v.getTypeAsString(), "", v.getDataAsString());
        }
        return result;
    }

    /**
     * Convert a `PIDRecord` instance to an array of `HandleValue`s
     * It is the inverse method to `pidRecordFrom`.
     * @param record the record containing values to convert / extract.
     * @return HandleValues containing the same key-value pairs as the given record,
     * but e.g. without the name.
     */
    protected HandleValue[] handleValuesFrom(PIDRecord record) {
        Map<String, List<PIDRecordEntry>> entries = record.getEntries();
        ArrayList<HandleValue> result = new ArrayList<HandleValue>();
        HandleIndex index = new HandleIndex();

        for (Entry<String, List<PIDRecordEntry>> entry : entries.entrySet()) {
            for (PIDRecordEntry val : entry.getValue()) {
                String key = val.getKey();
                HandleValue hv = new HandleValue();
                hv.setIndex(index.nextIndex());
                hv.setType(key.getBytes(StandardCharsets.UTF_8));
                hv.setData(val.getValue().getBytes(StandardCharsets.UTF_8));
                result.add(hv);
            }
        }
        return result.toArray(new HandleValue[]{});
    }

    protected static class HandleIndex {
        // handle record indices start at 1
        private int index = 1;

        public final int nextIndex() {
            int result = index;
            index += 1;
            if (index == this.getHsAdminIndex()) { index += 1; }
            return result;
        }

        public final int getHsAdminIndex() {
            return 100;
        }
    }

    /**
     * Generates a random PID. NOTE: Expects handleIdentifierPrefix in props to be set.
     * @return A random PID with the generator prefix from the preferences.
     */
    protected String generateRandomPID() {
        String uuid = UUID.randomUUID().toString();
        return this.props
            .getCredentials()
            .getHandleIdentifierPrefix()
            .concat("/")
            .concat(uuid);
    }

    /**
     * Returns true if the PID is valid according to the following criteria:
     * - PID is valid according to isIdentifierRegistered
     * - If a generator prefix is set, the PID is expedted to have this prefix.
     * @param pid the identifier / PID to check.
     * @return true if PID is registered (and if has the generatorPrefix, if it exists).
     */
    private boolean isValidPID(String pid) {
        boolean isAuthMode = this.props.getCredentials() != null;
        if (isAuthMode && !pid.startsWith(this.props.getCredentials().getHandleIdentifierPrefix())) {
            return false;
        }
        try {
            if (!this.isIdentifierRegistered(pid)) { return false; }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
