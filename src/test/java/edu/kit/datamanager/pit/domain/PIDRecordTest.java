package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Test Ensure the Entry of Insertion And Extraction of the class
 */

public class PIDRecordTest {
    private static final String PID = "fake/pid/42";

    @Test
    void assignPIDTest() {
        PIDRecord r = new PIDRecord().withPID(PID);
        assertEquals(PID, r.getPid());
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
        Map<String, List<PIDRecordEntry>> entries = rec.getEntries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).get(0).getName());
    }

    @Test
    void assignPID() {

        PIDRecord r = new PIDRecord().withPID(PID);
        assertEquals(PID, r.getPid());
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
        Map<String, List<PIDRecordEntry>> entries = rec.getEntries();
        assertEquals(1, entries.size());
        assertEquals(name, entries.get(identifier).get(0).getName());
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
        PIDRecord rec = new PIDRecord();
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
    void hasProperty_Test() {

        PIDRecord rec = new PIDRecord();
        boolean Expected_identifier = true;
        String identifier = "propertyIdentifier";
        String name = "propertyName";
        String value = "propertyValue";
        rec.addEntry(identifier, name, value);
        assertEquals(Expected_identifier, rec.hasProperty("propertyIdentifier"));

    }
}
