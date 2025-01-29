package edu.kit.datamanager.pit.resolver;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.handle.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Universal resolver. Will not only resolve PIDs from the configured PID system,
 * but also from other external systems, to which it has read-only access to.
 * <p>
 * Currently implemented read-only systems:
 * <p>
 * - Handle System
 */
public class Resolver {
    /**
     * The configured system to which we usually have write access.
     * Only used if the prefix of the PID matches the prefix of this system.
     */
    private final ITypingService identifierSystem;

    /**
     * The client to the Handle System, used in read-only mode.
     * Used as a fallback, if the prefix is not the one of the configured system.
     */
    private final HSAdapter client = HSAdapterFactory.newInstance();
    private static final String SERVICE_NAME_HANDLE = "Handle System (read-only access)";

    public Resolver(ITypingService identifierSystem) {
        this.identifierSystem = identifierSystem;
    }

    /**
     * Resolves a PID to a PIDRecord.
     * <p>
     * Takes advantage of administrative access to the configured PID system, if possible.
     * Otherwise, falls back to read-only access to external systems.
     *
     * @param pid the PID to resolve.
     * @return the PIDRecord associated with the PID.
     * @throws PidNotFoundException if the PID could not be found in any system.
     * @throws ExternalServiceException if there was an error with the communication to an external system.
     */
    public PIDRecord resolve(String pid) throws PidNotFoundException, ExternalServiceException {
        String prefix = Arrays.stream(
                pid.split("/", 2)
        )
                .findFirst()
                .orElseThrow(() -> new PidNotFoundException(pid, "Could not find prefix in PID."))
                + "/"; // needed because the prefix is always followed by a slash
        boolean isInConfiguredIdentifierSystem = this.identifierSystem != null && this.identifierSystem.getPrefix()
                .map(prefix::equals)
                .orElse(false);
        if (isInConfiguredIdentifierSystem) {
            return this.identifierSystem.queryPid(pid);
        } else {
            try {
                Collection<HandleValue> recordProperties = Arrays.stream(this.client.resolveHandle(pid, null, null))
                        .filter(value -> !HandleProtocolAdapter.isHandleInternalValue(value))
                        .collect(Collectors.toList());
                return new PIDRecord(recordProperties).withPID(pid);
            } catch (HandleException e) {
                int code = e.getCode();
                boolean isExistingPid = code == HandleException.HANDLE_DOES_NOT_EXIST;
                boolean missingPrefixHost = false;
                if (e.getCause() instanceof HandleException inner) {
                    int innerCode = inner.getCode();
                    missingPrefixHost = innerCode == HandleException.SERVICE_NOT_FOUND
                            || innerCode == HandleException.HANDLE_DOES_NOT_EXIST;
                }
                if (isExistingPid || missingPrefixHost) {
                    throw new PidNotFoundException(pid, e);
                } else {
                    throw new ExternalServiceException(SERVICE_NAME_HANDLE, e);
                }
            }
        }
    }
}
