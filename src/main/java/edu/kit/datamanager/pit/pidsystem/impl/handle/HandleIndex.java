package edu.kit.datamanager.pit.pidsystem.impl.handle;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows iterating over handle indices, skipping administrative ones.
 */
class HandleIndex {
    // handle record indices start at 1
    private int index = 1;
    private List<Integer> skipping = new ArrayList<>();

    public int nextIndex() {
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

    public int getHsAdminIndex() {
        return 100;
    }
}