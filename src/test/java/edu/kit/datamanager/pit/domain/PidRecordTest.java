package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Test Ensure the Entry of Insertion And Extraction of the class
 */

class PidRecordTest {
    private static final String PID = "fake/pid/42";

    @Test
    void assignPIDTest() {
        PidRecord r = new PidRecord().withPID(PID);
        assertEquals(PID, r.getPid());
    }

    @Test
    void testCorrectEntryInsertionAndExtraction() {
        PidRecord rec = new PidRecord();
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);

        // now we check if the class acts like expected.
        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);
        Map<String, List<PIDRecordEntry>> entries = rec.getEntries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).get(0).getName());
    }

    @Test
    void assignPID() {

        PidRecord r = new PidRecord().withPID(PID);
        assertEquals(PID, r.getPid());
    }

    @Test
    void testAddEntryWithEmptyIdentifier() {
        PidRecord rec = new PidRecord();
        String identifier = "";
        String name = "propertyName";
        String value = "propertyValue";
        assertThrows(
                IllegalArgumentException.class,
                () -> rec.addEntry(identifier, name, value));
    }

    @Test
    void testAddEntry_IdIsSpace() {
        // TODO Discuss, is this a good thing? should we change it?
        PidRecord rec = new PidRecord();
        String identifier = " ";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);

        // now we check if the class acts like expected.
        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);
        Map<String, List<PIDRecordEntry>> entries = rec.getEntries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).get(0).getName());
    }

    @Test
    void testAddEntry_IdIsNull() {
        PidRecord rec = new PidRecord();
        String identifier = null;
        String name = "propertyName";
        String value = "propertyValue";

        assertThrows(
                NullPointerException.class,
                () -> rec.addEntry(identifier, name, value));
    }

    @Test
    void testGetPropertyValuesWithSameIdentifier() {
        PidRecord rec = new PidRecord();
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);
        assertEquals(true, rec.hasProperty("propertyIdentifier"));

        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);

        String other_name = "otherName";
        String other_value = "otherValue";
        rec.addEntry(identifier, other_name, other_value);

        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedOtherValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedOtherValue);

        String[] expectedValues = { value, other_value };
        String[] receivedValues = rec.getPropertyValues(identifier);
        assertEquals(expectedValues[0], receivedValues[0]);
        assertEquals(expectedValues[1], receivedValues[1]);
        assertEquals(expectedValues.length, receivedValues.length);
    }

    @Test
    void testHasProperty() {
        PidRecord rec = new PidRecord();
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);
        assertTrue(rec.hasProperty("propertyIdentifier"));
    }

    @Test
    void testRemovePropertiesNotListed() {
        PidRecord rec = new PidRecord();
        String id1 = "propertyIdentifier";
        String id2 = "otherIdentifier";
        String name = "propertyName";
        String value1 = "propertyValue";
        String value2 = "otherValue";
        rec.addEntry(id1, name, value1);
        rec.addEntry(id1, name, value2);
        rec.addEntry(id2, name, value2);
        
        Set<String> propertiesToKeep = rec.getPropertyIdentifiers();
        rec.removePropertiesNotListed(propertiesToKeep);
        assertTrue(propertiesToKeep.contains(id1));
        assertTrue(propertiesToKeep.contains(id2));
        assertTrue(rec.hasProperty(id1));
        assertTrue(rec.hasProperty(id2));

        propertiesToKeep.remove(id1);
        rec.removePropertiesNotListed(propertiesToKeep);
        assertFalse(rec.hasProperty(id1));
    }

    @Test
    void testEqualityOfEmpty() {
        PidRecord first = new PidRecord();
        PidRecord second = new PidRecord();
        this.equals(first, second);

        PidRecord third = new PidRecord().withPID(null);
        PidRecord fourth = new PidRecord().withPID("");
        this.equals(third, fourth);
    }

    @Test
    void testEqualityViaPid() {
        PidRecord first = new PidRecord().withPID("pid");
        PidRecord second = new PidRecord().withPID("pid");
        this.equals(first, second);
    }

    @Test
    void testInequalityViaPid() {
        PidRecord first = new PidRecord();
        PidRecord second = new PidRecord().withPID("first");
        this.notEquals(first, second);

        PidRecord third = new PidRecord().withPID("other");
        this.notEquals(first, third);
        this.notEquals(second, third);
    }

    @Test
    void testEqualityAlthoughNamedAttribute() {
        PidRecord first = new PidRecord();
        PidRecord second = new PidRecord();
        PidRecord third = new PidRecord();

        first.addEntry("key", "name", "value");
        second.addEntry("key", null, "value");
        third.addEntry("key", "", "value");
        
        this.equals(first, second);
        this.equals(second, third);
    }

    @Test
    void testEqualityAlthoughDifferentOrder() {
        PidRecord first = new PidRecord();
        PidRecord second = new PidRecord();

        first.addEntry("key", "name", "value1");
        first.addEntry("key", "name", "value2");
        second.addEntry("key", "name", "value2");
        second.addEntry("key", "name", "value1");
        
        this.equals(first, second);
    }

    private void equals(PidRecord first, PidRecord second) {
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.getEtag(), second.getEtag());
    }

    private void notEquals(PidRecord first, PidRecord second) {
        assertNotEquals(first, second);
        assertNotEquals(second, first);
        assertNotEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first.getEtag(), second.getEtag());
    }
}
