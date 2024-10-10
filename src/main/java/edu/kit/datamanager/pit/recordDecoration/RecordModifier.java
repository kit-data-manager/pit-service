package edu.kit.datamanager.pit.recordDecoration;

import edu.kit.datamanager.pit.domain.PIDRecord;

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
    public PIDRecord apply(final PIDRecord record);
}
