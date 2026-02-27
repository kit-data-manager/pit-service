package edu.kit.datamanager.pit.pidsystem.impl.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.handle.hdllib.HandleValue;

class HandleDiffTest {
    @Test
    void testDiffOldRecordEmpty() {
        Map<Integer, HandleValue> oldRecord = new HashMap<>();
        Map<Integer, HandleValue> newRecord = new HashMap<>();
        addSomeHandleValue(newRecord, 1);
        addSomeHandleValue(newRecord, 2);
        addSomeHandleValue(newRecord, 100);
        HandleDiff diff = new HandleDiff(oldRecord, newRecord);
        assertEquals(0, diff.removed().length);
        assertEquals(0, diff.updated().length);
        assertEquals(newRecord.size(), diff.added().length);
    }

    @Test
    void testDiffNewRecordEmpty() {
        Map<Integer, HandleValue> oldRecord = new HashMap<>();
        addSomeHandleValue(oldRecord, 1);
        addSomeHandleValue(oldRecord, 2);
        addSomeHandleValue(oldRecord, 100);
        Map<Integer, HandleValue> newRecord = new HashMap<>();
        HandleDiff diff = new HandleDiff(oldRecord, newRecord);
        assertEquals(oldRecord.size(), diff.removed().length);
        assertEquals(0, diff.updated().length);
        assertEquals(0, diff.added().length);
    }

    @Test
    void testDiffAllUpdated() {
        Map<Integer, HandleValue> oldRecord = new HashMap<>();
        addSomeHandleValue(oldRecord, 1);
        addSomeHandleValue(oldRecord, 2);
        addSomeHandleValue(oldRecord, 100);
        Map<Integer, HandleValue> newRecord = new HashMap<>();
        addSomeHandleValue(newRecord, 1);
        addSomeHandleValue(newRecord, 2);
        addSomeHandleValue(newRecord, 100);

        HandleDiff diff = new HandleDiff(oldRecord, newRecord);
        assertEquals(0, diff.removed().length);
        assertEquals(oldRecord.size(), diff.updated().length);
        assertEquals(0, diff.added().length);
    }

    @Test
    void testDiffOneOfEachChange() {
        Map<Integer, HandleValue> oldRecord = new HashMap<>();
        addSomeHandleValue(oldRecord, 1);
        addSomeHandleValue(oldRecord, 2);
        Map<Integer, HandleValue> newRecord = new HashMap<>();
        // removed 1
        addSomeHandleValue(newRecord, 2);  // potentially changed 2
        addSomeHandleValue(newRecord, 100); // added 100

        HandleDiff diff = new HandleDiff(oldRecord, newRecord);
        assertEquals(1, diff.removed().length);
        assertEquals(1, diff.updated().length);
        assertEquals(1, diff.added().length);
    }

    private static void addSomeHandleValue(Map<Integer, HandleValue> record, int index) {
        record.put(index, getHandleValue(index));
    }

    private static HandleValue getHandleValue(int index) {
        return new HandleValue(index, "", "");
    }
}
