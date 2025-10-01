package edu.kit.datamanager.pit.pidsystem.impl.handle;

import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.domain.PIDRecordEntry;
import net.handle.hdllib.Common;
import net.handle.hdllib.HandleValue;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class defines helper functions and constants which define special
 * behavior in the Typed PID Maker context.
 * <p>
 * This has the purpose of reuse, e.g. in the resolver module, but also
 * separating the authentication and PID logic from certain behavior aspects.
 * <p>
 * In later aspects, this may be extended to implement an interface and use the
 * strategy pattern in order to make the behavior configurable.
 */
public class HandleBehavior {

    /**
     * A list of type codes which are considered "internal" or "handle-native"
     * and should not be exposed. Use `isHandleInternalValue` to check if a
     * value should be filtered out.
     */
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

    /**
     * This class is not meant to be instantiated.
     */
    private HandleBehavior() {}

    /**
     * Checks if a given value is considered an "internal" or "handle-native" value.
     * <p>
     * This may be used to filter out administrative information from a PID record.
     *
     * @param v the value to check.
     * @return true, if the value is considered "handle-native".
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
     * Convert a `PIDRecord` instance to an array of `HandleValue`s. It is the
     * inverse method to `pidRecordFrom`.
     *
     * @param pidRecord the record containing values to convert / extract.
     * @param toMerge   an optional list to merge the result with.
     * @return HandleValues containing the same key-value pairs as the given record,
     *         but e.g. without the name.
     */
    public static ArrayList<HandleValue> handleValuesFrom(
            final PidRecord pidRecord,
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

        for (Map.Entry<String, List<PIDRecordEntry>> entry : entries.entrySet()) {
            for (PIDRecordEntry val : entry.getValue()) {
                String key = val.getKey();
                HandleValue hv = new HandleValue();
                int i = index.nextIndex();
                hv.setIndex(i);
                hv.setType(key.getBytes(StandardCharsets.UTF_8));
                hv.setData(val.getValue().getBytes(StandardCharsets.UTF_8));
                result.add(hv);
            }
        }
        assert result.size() >= pidRecord.getEntries().size();
        return result;
    }

    public static PidRecord recordFrom(final Collection<HandleValue> values) {
        PidRecord record = new PidRecord();
        values.forEach(v -> record.addEntry(
                v.getTypeAsString(),
                v.getDataAsString())
        );
        return record;
    }
}
