package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidUpdateException;
import edu.kit.datamanager.pit.configuration.HandleCredentials;
import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.apps.batch.BatchUtil;
import net.handle.hdllib.Common;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.SiteInfo;

/**
 * Uses the official java library to interact with the handle system using the
 * handle protocol.
 */
@Component
@ConditionalOnBean(HandleProtocolProperties.class)
public class HandleProtocolAdapter implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(HandleProtocolProperties.class);

    private static final byte[][][] BLACKLIST_NONTYPE_LISTS = {
            Common.SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES,
            Common.DERIVED_PREFIX_SITE_AND_SERVICE_HANDLE_TYPES,
            Common.SERVICE_HANDLE_TYPES,
            Common.LOCATION_AND_ADMIN_TYPES,
            Common.SECRET_KEY_TYPES,
            Common.PUBLIC_KEY_TYPES,
            // Common.STD_TYPES, // not using because of URL and EMAIL
            {
                    // URL and EMAIL might contain valuable information and can be considered
                    // non-technical.
                    // Common.STD_TYPE_URL,
                    // Common.STD_TYPE_EMAIL,
                    Common.STD_TYPE_HSADMIN,
                    Common.STD_TYPE_HSALIAS,
                    Common.STD_TYPE_HSSITE,
                    Common.STD_TYPE_HSSITE6,
                    Common.STD_TYPE_HSSERV,
                    Common.STD_TYPE_HSSECKEY,
                    Common.STD_TYPE_HSPUBKEY,
                    Common.STD_TYPE_HSVALLIST,
            }
    };

    // Properties specific to this adapter.
    @Autowired
    private HandleProtocolProperties props;
    // Handle Protocol implementation
    private HSAdapter client;
    // indicates if the adapter can modify and create PIDs or just resolve them.
    private boolean isAdminMode = false;
    // the value that is appended to every new record.
    private HandleValue adminValue;

    // For testing
    public HandleProtocolAdapter(HandleProtocolProperties props) {
        this.props = props;
    }

    /**
     * Initializes internal classes.
     * We use this methos with the @PostConstruct annotation to run it
     * after the constructor and after springs autowiring is done properly
     * to make sure that all properties are already autowired.
     * 
     * @throws HandleException        if a handle system error occurs.
     * @throws InvalidConfigException if the configuration is invalid, e.g. a path
     *                                does not lead to a file.
     * @throws IOException            if the private key file can not be read.
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
            byte[] privateKey = extractPrivateKey(credentials);
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
                    passphrase // "use null for unencrypted keys"
            );
            HandleIndex indexManager = new HandleIndex();
            this.adminValue = this.client.createAdminValue(
                    props.getCredentials().getUserHandle(),
                    props.getCredentials().getPrivateKeyIndex(),
                    indexManager.getHsAdminIndex());
        }
    }

    @Override
    public boolean isIdentifierRegistered(final String pid) throws IOException {
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
    public PIDRecord queryAllProperties(final String pid) throws IOException {
        Collection<HandleValue> allValues = this.queryAllHandleValues(pid);
        if (allValues == null) {
            return null;
        }
        Collection<HandleValue> record_properties = Streams.stream(allValues.stream())
                .filter(value -> !this.isHandleInternalValue(value))
                .collect(Collectors.toList());
        return this.pidRecordFrom(record_properties).withPID(pid);
    }

    protected Collection<HandleValue> queryAllHandleValues(final String pid) throws IOException {
        try {
            HandleValue[] values = this.client.resolveHandle(pid, null, null);
            return Stream
                    .of(values)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return null;
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public String queryProperty(final String pid, final TypeDefinition typeDefinition) throws IOException {
        String[] typeArray = { typeDefinition.getIdentifier() };
        try {
            // TODO we assume here that the property only exists once, which will not be
            // true in every case.
            // The interface likely should be adjusted so we can return all types and do not
            // need to return a String.
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
    public String registerPID(final PIDRecord record) throws IOException {
        // Add admin value for configured user only
        // TODO add options to add additional adminValues e.g. for user lists?
        ArrayList<HandleValue> admin = new ArrayList<>();
        admin.add(this.adminValue);
        ArrayList<HandleValue> record_values = this.handleValuesFrom(record, Optional.of(admin));

        HandleValue[] values = record_values.toArray(new HandleValue[] {});
        assert values.length >= record.getEntries().keySet().size();

        boolean success = false;
        while (!success) {
            record.setPid(generateRandomPID());
            try {
                this.client.createHandle(record.getPid(), values);
                success = true;
            } catch (HandleException e) {
                if (e.getCode() == HandleException.HANDLE_ALREADY_EXISTS) {
                    // try the loop again
                    success = false; // (just to make 100% sure the loop will run again)
                    continue;
                } else {
                    // On other errors, we throw an exception.
                    throw new IOException(e);
                }
            }
        }
        return record.getPid();
    }

    @Override
    public boolean updatePID(PIDRecord record) throws IOException {
        if (!this.isValidPID(record.getPid())) {
            return false;
        }
        // We need to override the old record as the user has no possibility to update
        // single values, and matching is hard.
        // The API expects the user to insert what the result should be. Due to the
        // Handle Protocol client available
        // functions and the way the handle system works with indices (basically value
        // identifiers), we use this approach:
        // 1) from the old values, take all we want to keep.
        // 2) together with the user-given record, merge "valuesToKeep" to a list of
        // values with unique indices.
        // 3) see (by index) which values have to be added, deleted, or updated.
        // 4) then add, update, delete in this order.

        // index value
        Map<Integer, HandleValue> recordOld = this.queryAllHandleValues(record.getPid())
                .stream()
                .collect(Collectors.toMap(v -> v.getIndex(), v -> v));
        // Streams.stream makes a stream failable, i.e. allows filtering with
        // exceptions. A new Java version **might** solve this.
        List<HandleValue> valuesToKeep = Streams.stream(this.queryAllHandleValues(record.getPid()).stream())
                .filter(v -> this.isHandleInternalValue(v))
                .collect(Collectors.toList());

        // Merge requested record and things we want to keep.
        Map<Integer, HandleValue> recordNew = handleValuesFrom(record, Optional.of(valuesToKeep))
                .stream()
                .collect(Collectors.toMap(v -> v.getIndex(), v -> v));

        try {
            HandleDiff diff = new HandleDiff(recordOld, recordNew);
            if (diff.added().length > 0) {
                this.client.addHandleValues(record.getPid(), diff.added());
            }
            if (diff.updated().length > 0) {
                this.client.updateHandleValues(record.getPid(), diff.updated());
            }
            if (diff.removed().length > 0) {
                this.client.deleteHandleValues(record.getPid(), diff.removed());
            }
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new IOException(e);
            }
        } catch (Exception e) {
            throw new IOException("Implementation error in calculating record difference.", e);
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
    public boolean deletePID(final String pid) throws IOException {
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

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws IOException, InvalidConfigException {
        HandleCredentials handleCredentials = this.props.getCredentials();

        PrivateKey key;
        {
            byte[] privateKeyBytes = this.extractPrivateKey(handleCredentials);
            byte[] passphrase = handleCredentials.getPrivateKeyPassphrase().getBytes();
            // decrypt the private key using the passphrase/cypher
            byte[] privateKeyDecrypted;
            try {
                privateKeyDecrypted = net.handle.hdllib.Util.decrypt(privateKeyBytes, passphrase);
                key = net.handle.hdllib.Util.getPrivateKeyFromBytes(privateKeyDecrypted, 0);
            } catch (Exception e) {
                throw new InvalidConfigException("Private key decryption failed: " + e.getMessage());
            }
        }

        PublicKeyAuthenticationInfo auth = new PublicKeyAuthenticationInfo(
                net.handle.hdllib.Util.encodeString(handleCredentials.getUserHandle()),
                handleCredentials.getPrivateKeyIndex(),
                key);

        HandleResolver resolver = new HandleResolver();
        SiteInfo site;
        {
            HandleValue[] prefixValues;
            try {
                prefixValues = resolver.resolveHandle(handleCredentials.getHandleIdentifierPrefix());
                site = BatchUtil.getFirstPrimarySiteFromHserv(prefixValues, resolver);
            } catch (HandleException e) {
                throw new IOException(e.getMessage());
            }
        }

        String prefix = handleCredentials.getHandleIdentifierPrefix();
        try {
            return BatchUtil.listHandles(prefix, site, resolver, auth);
        } catch (HandleException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Avoids an extra constructor in `PIDRecord`. Instead,
     * keep such details stored in the PID service implementation.
     * 
     * @param values HandleValue collection (ordering recommended)
     *               that shall be converted into a PIDRecord.
     * @return a PID record with values copied from values.
     */
    protected PIDRecord pidRecordFrom(final Collection<HandleValue> values) {
        PIDRecord result = new PIDRecord();
        for (HandleValue v : values) {
            // TODO In future, the type could be resolved to store the human readable name
            // here.
            result.addEntry(v.getTypeAsString(), "", v.getDataAsString());
        }
        return result;
    }

    /**
     * Convert a `PIDRecord` instance to an array of `HandleValue`s
     * It is the inverse method to `pidRecordFrom`.
     * 
     * @param record the record containing values to convert / extract.
     * @return HandleValues containing the same key-value pairs as the given record,
     *         but e.g. without the name.
     */
    protected ArrayList<HandleValue> handleValuesFrom(final PIDRecord record,
            final Optional<List<HandleValue>> toMerge) {
        ArrayList<Integer> skipping_indices = new ArrayList<Integer>();
        ArrayList<HandleValue> result = new ArrayList<HandleValue>();
        if (toMerge.isPresent()) {
            for (HandleValue v : toMerge.get()) {
                result.add(v);
                skipping_indices.add(v.getIndex());
            }
        }
        HandleIndex index = new HandleIndex().skipping(skipping_indices);
        Map<String, List<PIDRecordEntry>> entries = record.getEntries();

        for (Entry<String, List<PIDRecordEntry>> entry : entries.entrySet()) {
            for (PIDRecordEntry val : entry.getValue()) {
                String key = val.getKey();
                HandleValue hv = new HandleValue();
                int i = index.nextIndex();
                hv.setIndex(i);
                hv.setType(key.getBytes(StandardCharsets.UTF_8));
                hv.setData(val.getValue().getBytes(StandardCharsets.UTF_8));
                result.add(hv);
                LOG.debug("Entry: ({}) {} <-> {}", i, key, val);
            }
        }
        assert result.size() >= record.getEntries().keySet().size();
        return result;
    }

    protected static class HandleIndex {
        // handle record indices start at 1
        private int index = 1;
        private List<Integer> skipping = new ArrayList<Integer>();

        public final int nextIndex() {
            int result = index;
            index += 1;
            if (index == this.getHsAdminIndex() || skipping.contains(index)) {
                index += 1;
            }
            return result;
        }

        public HandleIndex skipping(List<Integer> skipThose) {
            this.skipping = skipThose;
            return this;
        }

        public final int getHsAdminIndex() {
            return 100;
        }
    }

    /**
     * Generates a random PID. NOTE: Expects handleIdentifierPrefix in props to be
     * set.
     * 
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
     * 
     * @param pid the identifier / PID to check.
     * @return true if PID is registered (and if has the generatorPrefix, if it
     *         exists).
     */
    protected boolean isValidPID(final String pid) {
        boolean isAuthMode = this.props.getCredentials() != null;
        if (isAuthMode && !pid.startsWith(this.props.getCredentials().getHandleIdentifierPrefix())) {
            return false;
        }
        try {
            if (!this.isIdentifierRegistered(pid)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    protected boolean isHandleInternalValue(HandleValue v) throws IOException {
        boolean isInternalValue = false; // !this.isIdentifierRegistered(v.getTypeAsString());
        for (byte[][] typeList : BLACKLIST_NONTYPE_LISTS) {
            for (byte[] typeCode : typeList) {
                isInternalValue = isInternalValue || Arrays.equals(v.getType(), typeCode);
            }
        }
        return isInternalValue;
    }

    /**
     * Extract bytes from private key. Might be encrypted.
     * 
     * @param credentials the handle credentials.
     * @return the bytes fron the private key file.
     * @throws IOException on error when reading from file.
     */
    protected byte[] extractPrivateKey(HandleCredentials credentials) throws IOException {
        {
            File maybeKeyFile = credentials.getPrivateKeyPath().toFile();
            if (!maybeKeyFile.exists()) {
                throw new InvalidConfigException(
                        String.format("PrivateKeyFilePath does not lead to a file: %s", maybeKeyFile.toString()));
            }
            if (!maybeKeyFile.isFile()) {
                throw new InvalidConfigException(
                        String.format("File to private key not a regular file: %s", maybeKeyFile.toString()));
            }
        }
        // NOTE We can still fail later if the private key file contains garbage.

        // Extract information and start handle client with available authentication.
        return Files.readAllBytes(credentials.getPrivateKeyPath());
    }

    /**
     * Given two Value Maps, it splits the values in those which have been added,
     * updated or removed.
     * Using this lists, an update can be applied to the old record, to bring it to
     * the state of the new record.
     */
    protected static class HandleDiff {
        private final Collection<HandleValue> toAdd = new ArrayList<>();
        private final Collection<HandleValue> toUpdate = new ArrayList<>();
        private final Collection<HandleValue> toRemove = new ArrayList<>();

        HandleDiff(final Map<Integer, HandleValue> recordOld, final Map<Integer, HandleValue> recordNew)
                throws Exception {
            // old_indexes should only contain indexes we do not override/update anyway, so
            // we can delete them afterwards.
            for (Entry<Integer, HandleValue> old : recordOld.entrySet()) {
                boolean wasRemoved = !recordNew.containsKey(old.getKey());
                if (wasRemoved) {
                    toRemove.add(old.getValue());
                } else {
                    toUpdate.add(recordNew.get(old.getKey()));
                }
            }
            for (Entry<Integer, HandleValue> e : recordNew.entrySet()) {
                boolean isNew = !recordOld.containsKey(e.getKey());
                if (isNew) {
                    toAdd.add(e.getValue());
                }
            }

            // runtime testing to avoid messing up record states.
            String exception_msg = "DIFF NOT VALID. Type: %s. Value: %s";
            for (HandleValue v : toRemove) {
                boolean valid = recordOld.containsValue(v) && !recordNew.containsKey(v.getIndex());
                if (!valid) {
                    String message = String.format(exception_msg, "Remove", v.toString());
                    throw new PidUpdateException(message);
                }
            }
            for (HandleValue v : toAdd) {
                boolean valid = !recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
                if (!valid) {
                    String message = String.format(exception_msg, "Add", v.toString());
                    throw new PidUpdateException(message);
                }
            }
            for (HandleValue v : toUpdate) {
                boolean valid = recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
                if (!valid) {
                    String message = String.format(exception_msg, "Update", v.toString());
                    throw new PidUpdateException(message);
                }
            }
        }

        public HandleValue[] added() {
            return this.toAdd.toArray(new HandleValue[] {});
        }

        public HandleValue[] updated() {
            return this.toUpdate.toArray(new HandleValue[] {});
        }

        public HandleValue[] removed() {
            return this.toRemove.toArray(new HandleValue[] {});
        }
    }
}
