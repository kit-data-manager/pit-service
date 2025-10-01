package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

// We assume that two PidNode are equal if their PidRecord parts are equal.
// The "exists" field is not making a difference for content equality.
// Therefore, just keep the "equal"-implementation from PidRecord.
@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class PidNode extends PidRecord {
    @JsonProperty("placeholder_pid")
    private String placeholderPid;

    public

    @Override
    public PidNode clone() {
        PidNode clone = (PidNode) super.clone();
        clone.placeholderPid = this.placeholderPid;
        return clone;
    }
}
