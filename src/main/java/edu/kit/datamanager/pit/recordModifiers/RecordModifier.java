package edu.kit.datamanager.pit.recordModifiers;

import edu.kit.datamanager.pit.domain.PidRecord;

/**
 * An interface for types which allow for modification of PID records.
 */
public interface RecordModifier {
    /**
     * Apply the modifier to the given record.
     *
     * @param record the record to modify
     * @return the modified record (same as input).
     */
    public PidRecord apply(final PidRecord record);
}
