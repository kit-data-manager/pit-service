package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.domain.PidRecord;

public record RegisteredProfileAttribute(
        String pid,
        boolean mandatory,
        boolean repeatable
) {
    public boolean violatesMandatoryProperty(PidRecord pidRecord) {
        boolean contains = pidRecord.getPropertyIdentifiers().contains(this.pid)
                && pidRecord.getPropertyValues(this.pid).length > 0;
        return this.mandatory && !contains;
    }

    public boolean violatesRepeatableProperty(PidRecord pidRecord) {
        boolean repeats = pidRecord.getPropertyValues(this.pid).length > 1;
        return !this.repeatable && repeats;
    }
}
