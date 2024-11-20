package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;

public record RegisteredProfileAttribute(
        String pid,
        boolean mandatory,
        boolean repeatable
) {
    public boolean violatesMandatoryProperty(PIDRecord pidRecord) {
        boolean contains = pidRecord.getPropertyIdentifiers().contains(this.pid)
                && pidRecord.getPropertyValues(this.pid).length > 0;
        return this.mandatory && !contains;
    }

    public boolean violatesRepeatableProperty(PIDRecord pidRecord) {
        boolean repeats = pidRecord.getPropertyValues(this.pid).length > 1;
        return !this.repeatable && repeats;
    }
}
