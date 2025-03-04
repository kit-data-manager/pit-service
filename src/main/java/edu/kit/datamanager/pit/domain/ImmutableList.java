package edu.kit.datamanager.pit.domain;

import java.util.Collections;
import java.util.List;

public record ImmutableList<I>(List<I> items) {
    public ImmutableList {
        items = Collections.unmodifiableList(items);
    }
}