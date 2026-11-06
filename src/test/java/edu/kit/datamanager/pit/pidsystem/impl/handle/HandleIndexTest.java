package edu.kit.datamanager.pit.pidsystem.impl.handle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandleIndexTest {

    @Test
    void isSkippingDefaultAdminIndex() {
        HandleIndex handleIndex = new HandleIndex();
        for (int value = 1; value <= 200; value++) {
            if (value >= handleIndex.getHsAdminIndex()) {
                assertEquals(value + 1, handleIndex.nextIndex());
            } else {
                assertEquals(value, handleIndex.nextIndex());
            }
        }
    }

    @Test
    void isSkippingList() {
        List<Integer> skipping = List.of(3, 10, 42, 1337);
        HandleIndex handleIndex = new HandleIndex().skipping(skipping);

        int lastValue = 0;
        for (int _i = 1; _i <= 200; _i++) {
            int value = handleIndex.nextIndex();
            assertTrue(value != handleIndex.getHsAdminIndex());
            for (int skip : skipping) {
                assertNotEquals(value, skip);
            }
            assertTrue(value > lastValue);
            lastValue = value;
        }
    }
}