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

class PIDRecordTest {
    private static final String PID = "fake/pid/42";

    @Test
    void assignPIDTest() {
        PIDRecord r = new PIDRecord().withPID(PID);
        assertEquals(PID, r.pid());
    }

    @Test
    void testCorrectEntryInsertionAndExtraction() {
        PIDRecord rec = new PIDRecord();
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);

        // now we check if the class acts like expected.
        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);
        Map<String, List<PidRecordEntry>> entries = rec.entries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).getFirst().name());
    }

    @Test
    void assignPID() {

        PIDRecord r = new PIDRecord().withPID(PID);
        assertEquals(PID, r.pid());
    }

    @Test
    void testAddEntryWithEmptyIdentifier() {
        PIDRecord rec = new PIDRecord();
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
        PIDRecord rec = new PIDRecord();
        String identifier = " ";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);

        // now we check if the class acts like expected.
        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);
        Map<String, List<PidRecordEntry>> entries = rec.entries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).getFirst().name());
    }

    @Test
    void testAddEntry_IdIsNull() {
        PIDRecord rec = new PIDRecord();
        String identifier = null;
        String name = "propertyName";
        String value = "propertyValue";

        assertThrows(
                NullPointerException.class,
                () -> rec.addEntry(identifier, name, value));
    }

    @Test
    void testGetPropertyValuesWithSameIdentifier() {
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        PIDRecord rec = new PIDRecord().addEntry(identifier, name, value);
        assertTrue(rec.hasProperty("propertyIdentifier"));

        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedValue);

        String other_name = "otherName";
        String other_value = "otherValue";
        rec = rec.addEntry(identifier, other_name, other_value);

        assertTrue(rec.getPropertyIdentifiers().contains(identifier));
        String receivedOtherValue = rec.getPropertyValue(identifier);
        assertEquals(value, receivedOtherValue);

        String[] expectedValues = { value, other_value };
        List<String> receivedValues = rec.getPropertyValues(identifier);
        assertEquals(expectedValues[0], receivedValues.get(0));
        assertEquals(expectedValues[1], receivedValues.get(1));
        assertEquals(expectedValues.length, receivedValues.size());
    }

    @Test
    void testHasProperty() {
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        PIDRecord rec = new PIDRecord().addEntry(identifier, name, value);
        assertTrue(rec.hasProperty("propertyIdentifier"));
    }

    @Test
    void testRemovePropertiesNotListed() {
        String id1 = "propertyIdentifier";
        String id2 = "otherIdentifier";
        String name = "propertyName";
        String value1 = "propertyValue";
        String value2 = "otherValue";
        PIDRecord rec = new PIDRecord()
                .addEntry(id1, name, value1)
                .addEntry(id1, name, value2)
                .addEntry(id2, name, value2);
        
        Set<String> propertiesToKeep = rec.getPropertyIdentifiers();
        rec = rec.removePropertiesNotListed(propertiesToKeep);
        assertTrue(propertiesToKeep.contains(id1));
        assertTrue(propertiesToKeep.contains(id2));
        assertTrue(rec.hasProperty(id1));
        assertTrue(rec.hasProperty(id2));

        propertiesToKeep.remove(id1);
        rec = rec.removePropertiesNotListed(propertiesToKeep);
        assertFalse(rec.hasProperty(id1));
    }

    @Test
    void testEqualityOfEmpty() {
        PIDRecord first = new PIDRecord();
        PIDRecord second = new PIDRecord();
        this.equals(first, second);

        PIDRecord third = new PIDRecord().withPID(null);
        PIDRecord fourth = new PIDRecord().withPID("");
        this.equals(third, fourth);
    }

    @Test
    void testEqualityViaPid() {
        PIDRecord first = new PIDRecord().withPID("pid");
        PIDRecord second = new PIDRecord().withPID("pid");
        this.equals(first, second);
    }

    @Test
    void testInequalityViaPid() {
        PIDRecord first = new PIDRecord();
        PIDRecord second = new PIDRecord().withPID("first");
        this.notEquals(first, second);

        PIDRecord third = new PIDRecord().withPID("other");
        this.notEquals(first, third);
        this.notEquals(second, third);
    }

    @Test
    void testEqualityAlthoughNamedAttribute() {
        PIDRecord first = new PIDRecord().addEntry("key", "name", "value");
        PIDRecord second = new PIDRecord().addEntry("key", null, "value");
        PIDRecord third = new PIDRecord().addEntry("key", "", "value");
        this.equals(first, second);
        this.equals(second, third);
    }

    @Test
    void testEqualityAlthoughDifferentOrder() {
        PIDRecord first = new PIDRecord()
                .addEntry("key", "name", "value1")
                .addEntry("key", "name", "value2");
        PIDRecord second = new PIDRecord()
                .addEntry("key", "name", "value2")
                .addEntry("key", "name", "value1");
        this.equals(first, second);
    }

    private void equals(PIDRecord first, PIDRecord second) {
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.getEtag(), second.getEtag());
    }

    private void notEquals(PIDRecord first, PIDRecord second) {
        assertNotEquals(first, second);
        assertNotEquals(second, first);
        assertNotEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first.getEtag(), second.getEtag());
    }
}
