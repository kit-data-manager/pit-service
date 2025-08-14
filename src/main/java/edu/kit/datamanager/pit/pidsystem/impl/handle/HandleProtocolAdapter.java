/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kit.datamanager.pit.pidsystem.impl.handle;

import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.configuration.HandleCredentials;
import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.recordModifiers.RecordModifier;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.apps.batch.BatchUtil;
import net.handle.hdllib.*;
import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Uses the official java library to interact with the handle system using the
 * handle protocol.
 */
@Component
@ConditionalOnBean(HandleProtocolProperties.class)
@Observed
public class HandleProtocolAdapter implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(HandleProtocolAdapter.class);

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
            this.adminValue = this.client.createAdminValue(
                    props.getCredentials().getUserHandle(),
                    props.getCredentials().getPrivateKeyIndex(),
                    new HandleIndex().getHsAdminIndex());
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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_is_pid_registered", description = "Time taken to check if PID is registered in Handle system")
    @Counted(value = "handle_system_is_pid_registered_total", description = "Total number of PID registration checks")
    public boolean isPidRegistered(@SpanAttribute final String pid) throws ExternalServiceException {
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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_query_pid", description = "Time taken to query PID from Handle system")
    @Counted(value = "handle_system_query_pid_total", description = "Total number of PID queries")
    public PIDRecord queryPid(@SpanAttribute final String pid) throws PidNotFoundException, ExternalServiceException {
        Collection<HandleValue> allValues = this.queryAllHandleValues(pid);
        if (allValues.isEmpty()) {
            return null;
        }
        Collection<HandleValue> recordProperties = Streams.failableStream(allValues.stream())
                .filter(value -> !HandleBehavior.isHandleInternalValue(value))
                .collect(Collectors.toList());
        return HandleBehavior.recordFrom(recordProperties).withPID(pid);
    }

    @NotNull
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "handle_system_query_all_values", description = "Time taken to query all handle values")
    protected Collection<HandleValue> queryAllHandleValues(@SpanAttribute final String pid) throws PidNotFoundException, ExternalServiceException {
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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_register_pid", description = "Time taken to register PID in Handle system")
    @Counted(value = "handle_system_register_pid_total", description = "Total number of PID registrations")
    public String registerPidUnchecked(@SpanAttribute final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        // Add admin value for configured user only
        // TODO add options to add additional adminValues e.g. for user lists?
        ArrayList<HandleValue> admin = new ArrayList<>();
        admin.add(this.adminValue);
        PIDRecord preparedRecord = pidRecord;
        for (RecordModifier modifier : this.props.getConfiguredModifiers()) {
            preparedRecord = modifier.apply(preparedRecord);
        }
        ArrayList<HandleValue> futurePairs = HandleBehavior.handleValuesFrom(preparedRecord, Optional.of(admin));

        HandleValue[] futurePairsArray = futurePairs.toArray(new HandleValue[]{});

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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_update_pid", description = "Time taken to update PID in Handle system")
    @Counted(value = "handle_system_update_pid_total", description = "Total number of PID updates")
    public boolean updatePid(@SpanAttribute final PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
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
                .filter(HandleBehavior::isHandleInternalValue)
                .collect(Collectors.toList());

        // 2) Merge requested record and things we want to keep.
        Map<Integer, HandleValue> recordNew = HandleBehavior.handleValuesFrom(preparedRecord, Optional.of(valuesToKeep))
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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_delete_pid", description = "Time taken to delete PID from Handle system")
    @Counted(value = "handle_system_delete_pid_total", description = "Total number of PID deletions")
    public boolean deletePid(@SpanAttribute final String pid) throws ExternalServiceException {
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
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed(value = "handle_system_resolve_all_pids", description = "Time taken to resolve all PIDs from Handle system")
    @Counted(value = "handle_system_resolve_all_pids_total", description = "Total number of resolve all PIDs requests")
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
     * Returns true if the PID is valid according to the following criteria:
     * - PID is valid according to isIdentifierRegistered
     * - If a generator prefix is set, the PID is expeted to have this prefix.
     *
     * @param pid the identifier / PID to check.
     * @return true if PID is registered (and if has the generatorPrefix, if it
     * exists).
     */
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "handle_system_is_valid_pid", description = "Time taken to validate PID")
    protected boolean isValidPID(@SpanAttribute final String pid) {
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
}
