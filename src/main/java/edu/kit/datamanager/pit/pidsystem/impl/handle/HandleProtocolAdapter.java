package edu.kit.datamanager.pit.pidsystem.impl.handle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.kit.datamanager.pit.recordModifiers.RecordModifier;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.PidUpdateException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.HandleCredentials;
import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
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
import net.handle.hdllib.Util;

/**
 * Uses the official java library to interact with the handle system using the
 * handle protocol.
 */
@Component
@ConditionalOnBean(HandleProtocolProperties.class)
public class HandleProtocolAdapter implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(HandleProtocolAdapter.class);

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

    private static final String SERVICE_NAME_HANDLE = "Handle System";

    // Properties specific to this adapter.
    @Autowired
    final private HandleProtocolProperties props;
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
     * We use this method with the @PostConstruct annotation to run it
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
            byte[] privateKey = credentials.getPrivateKeyFileContent();
            byte[] passphrase = credentials.getPrivateKeyPassphraseAsBytes();
            LOG.debug("Logging in with user {}", credentials.getUserHandle());
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
    public Optional<String> getPrefix() {
        if (this.isAdminMode) {
            return Optional.ofNullable(this.props.getCredentials()).map(HandleCredentials::getHandleIdentifierPrefix);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean isPidRegistered(final String pid) throws ExternalServiceException {
        HandleValue[] recordProperties;
        try {
            recordProperties = this.client.resolveHandle(pid, null, null);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        }
        return recordProperties != null && recordProperties.length > 0;
    }

    @Override
    public PIDRecord queryPid(final String pid) throws PidNotFoundException, ExternalServiceException {
        Collection<HandleValue> allValues = this.queryAllHandleValues(pid);
        if (allValues.isEmpty()) {
            return null;
        }
        Collection<HandleValue> recordProperties = Streams.failableStream(allValues.stream())
                .filter(value -> !isHandleInternalValue(value))
                .collect(Collectors.toList());
        return new PIDRecord(recordProperties).withPID(pid);
    }

    @NotNull
    protected Collection<HandleValue> queryAllHandleValues(final String pid) throws PidNotFoundException, ExternalServiceException {
        try {
            HandleValue[] values = this.client.resolveHandle(pid, null, null);
            return Stream.of(values)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                throw new PidNotFoundException(pid, e);
            } else {
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        }
    }

    @Override
    public String registerPidUnchecked(final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        // Add admin value for configured user only
        // TODO add options to add additional adminValues e.g. for user lists?
        ArrayList<HandleValue> admin = new ArrayList<>();
        admin.add(this.adminValue);
        PIDRecord preparedRecord = pidRecord;
        for (RecordModifier modifier : this.props.getConfiguredModifiers()) {
            preparedRecord = modifier.apply(preparedRecord);
        }
        ArrayList<HandleValue> futurePairs = handleValuesFrom(preparedRecord, Optional.of(admin));

        HandleValue[] futurePairsArray = futurePairs.toArray(new HandleValue[] {});

        try {
            this.client.createHandle(preparedRecord.getPid(), futurePairsArray);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_ALREADY_EXISTS) {
                // Should not happen as this has to be checked on the REST handler level.
                throw new PidAlreadyExistsException(preparedRecord.getPid());
            } else {
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        }
        return preparedRecord.getPid();
    }

    @Override
    public boolean updatePid(final PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        if (!this.isValidPID(pidRecord.getPid())) {
            return false;
        }
        PIDRecord preparedRecord = pidRecord;
        for (RecordModifier modifier : this.props.getConfiguredModifiers()) {
            preparedRecord = modifier.apply(preparedRecord);
        }
        // We need to override the old record as the user has no possibility to update
        // single values, and matching is hard.
        // The API expects the user to insert what the result should be. Due to the
        // Handle Protocol client available
        // functions and the way the handle system works with indices (basically value
        // identifiers), we use this approach:
        // 1) from the old values, take all we want to keep (handle internal values, technical stuff).
        // 2) together with the user-given record, merge "valuesToKeep" to a list of
        // values with unique indices. Now we have exactly the representation we want.
        // But: we cannot tell the handle API what we want, we have to declare how to do it.
        // This is why we need two more steps:
        // 3) see (by index) which values have to be added, deleted, or updated.
        // 4) then add, update, delete in this order. Why this order? We could also remove everything
        // at first and then add everything we want, but this would require more actions on the server
        // side. And, deleting everything would also delete access control information. So, the safe
        // way to do it, is to add things which do not exist yet, update what needs to be updated,
        // and in the end remove what needs to be removed (usually nothing!).

        // index value
        Collection<HandleValue> oldHandleValues = this.queryAllHandleValues(preparedRecord.getPid());
        Map<Integer, HandleValue> recordOld = oldHandleValues.stream()
                .collect(Collectors.toMap(HandleValue::getIndex, v -> v));
        // 1)
        List<HandleValue> valuesToKeep = oldHandleValues.stream()
                .filter(HandleProtocolAdapter::isHandleInternalValue)
                .collect(Collectors.toList());

        // 2) Merge requested record and things we want to keep.
        Map<Integer, HandleValue> recordNew = handleValuesFrom(preparedRecord, Optional.of(valuesToKeep))
                .stream()
                .collect(Collectors.toMap(HandleValue::getIndex, v -> v));

        try {
            // 3)
            HandleDiff diff = new HandleDiff(recordOld, recordNew);
            // 4)
            if (diff.added().length > 0) {
                this.client.addHandleValues(preparedRecord.getPid(), diff.added());
            }
            if (diff.updated().length > 0) {
                this.client.updateHandleValues(preparedRecord.getPid(), diff.updated());
            }
            if (diff.removed().length > 0) {
                this.client.deleteHandleValues(preparedRecord.getPid(), diff.removed());
            }
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Implementation error in calculating record difference. PLEASE REPORT!", e);
        }
        return true;
    }

    @Override
    public boolean deletePid(final String pid) throws ExternalServiceException {
        try {
            this.client.deleteHandle(pid);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return false;
            } else {
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        }
        return true;
    }

    @Override
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        HandleCredentials handleCredentials = this.props.getCredentials();
        if (handleCredentials == null) {
            throw new InvalidConfigException("No credentials for handle protocol configured.");
        }

        PrivateKey key;
        {
            byte[] privateKeyBytes;
            try {
                privateKeyBytes = handleCredentials.getPrivateKeyFileContent();
            } catch (IOException e) {
                throw new InvalidConfigException("Could not read private key file content.");
            }
            if (privateKeyBytes == null || privateKeyBytes.length == 0) {
                throw new InvalidConfigException("Private Key is empty!");
            }
            byte[] passphrase = handleCredentials.getPrivateKeyPassphraseAsBytes();
            byte[] privateKeyDecrypted;
            // decrypt the private key using the passphrase/cypher
            try {
                privateKeyDecrypted = Util.decrypt(privateKeyBytes, passphrase);
            } catch (Exception e) {
                throw new InvalidConfigException("Private key decryption failed: " + e.getMessage());
            }
            try {
                key = Util.getPrivateKeyFromBytes(privateKeyDecrypted, 0);
            } catch (HandleException | InvalidKeySpecException e) {
                throw new InvalidConfigException("Private key conversion failed: " + e.getMessage());
            }
        }

        PublicKeyAuthenticationInfo auth = new PublicKeyAuthenticationInfo(
                Util.encodeString(handleCredentials.getUserHandle()),
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
                throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
            }
        }

        String prefix = handleCredentials.getHandleIdentifierPrefix();
        try {
            return BatchUtil.listHandles(prefix, site, resolver, auth);
        } catch (HandleException e) {
            throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
        }
    }

    /**
     * Convert a `PIDRecord` instance to an array of `HandleValue`s. It is the
     * inverse method to `pidRecordFrom`.
     * 
     * @param pidRecord the record containing values to convert / extract.
     * @param toMerge   an optional list to merge the result with.
     * @return HandleValues containing the same key-value pairs as the given record,
     *         but e.g. without the name.
     */
    protected static ArrayList<HandleValue> handleValuesFrom(
            final PIDRecord pidRecord,
            final Optional<List<HandleValue>> toMerge)
        {
        ArrayList<Integer> skippingIndices = new ArrayList<>();
        ArrayList<HandleValue> result = new ArrayList<>();
        if (toMerge.isPresent()) {
            for (HandleValue v : toMerge.get()) {
                result.add(v);
                skippingIndices.add(v.getIndex());
            }
        }
        HandleIndex index = new HandleIndex().skipping(skippingIndices);
        Map<String, List<PIDRecordEntry>> entries = pidRecord.getEntries();

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
        assert result.size() >= pidRecord.getEntries().size();
        return result;
    }

    protected static class HandleIndex {
        // handle record indices start at 1
        private int index = 1;
        private List<Integer> skipping = new ArrayList<>();

        public final int nextIndex() {
            int result = index;
            do {
                index += 1;
            } while (index == this.getHsAdminIndex() || skipping.contains(index));
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
            if (!this.isPidRegistered(pid)) {
                return false;
            }
        } catch (ExternalServiceException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a given value is considered an "internal" or "handle-native" value.
     * <p>
     * This may be used to filter out administrative information from a PID record.
     * 
     * @param v the value to check.
     * @return true, if the value is conidered "handle-native".
     */
    public static boolean isHandleInternalValue(HandleValue v) {
        boolean isInternalValue = false;
        for (byte[][] typeList : BLACKLIST_NONTYPE_LISTS) {
            for (byte[] typeCode : typeList) {
                isInternalValue = isInternalValue || Arrays.equals(v.getType(), typeCode);
            }
        }
        return isInternalValue;
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

        HandleDiff(
            final Map<Integer, HandleValue> recordOld,
            final Map<Integer, HandleValue> recordNew
        ) throws PidUpdateException {
            for (Entry<Integer, HandleValue> old : recordOld.entrySet()) {
                boolean wasRemoved = !recordNew.containsKey(old.getKey());
                if (wasRemoved) {
                    // if a row in the record is not available anymore, we need to delete it
                    toRemove.add(old.getValue());
                } else {
                    // otherwise, we should go and update it.
                    // we could also check for equality, but this is the safe and easy way.
                    // (the handlevalue classes can be complicated and we'd have to check their
                    // equality implementation)
                    toUpdate.add(recordNew.get(old.getKey()));
                }
            }
            for (Entry<Integer, HandleValue> e : recordNew.entrySet()) {
                boolean isNew = !recordOld.containsKey(e.getKey());
                if (isNew) {
                    // if there is a record which is not in the oldRecord, we need to add it.
                    toAdd.add(e.getValue());
                }
            }

            // runtime testing to avoid messing up record states.
            String exceptionMsg = "DIFF NOT VALID. Type: %s. Value: %s";
            for (HandleValue v : toRemove) {
                boolean valid = recordOld.containsValue(v) && !recordNew.containsKey(v.getIndex());
                if (!valid) {
                    String message = String.format(exceptionMsg, "Remove", v.toString());
                    throw new PidUpdateException(message);
                }
            }
            for (HandleValue v : toAdd) {
                boolean valid = !recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
                if (!valid) {
                    String message = String.format(exceptionMsg, "Add", v);
                    throw new PidUpdateException(message);
                }
            }
            for (HandleValue v : toUpdate) {
                boolean valid = recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
                if (!valid) {
                    String message = String.format(exceptionMsg, "Update", v);
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
