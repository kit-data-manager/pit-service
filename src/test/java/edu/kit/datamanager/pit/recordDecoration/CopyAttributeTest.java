package edu.kit.datamanager.pit.recordDecoration;

import edu.kit.datamanager.pit.domain.PIDRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CopyAttributeTest {
    public static final String SOURCE = "digitalObjectLocation";
    public static final String URL_VALUE = "https://example.com/mydata";
    public static final String SOURCE_2 = "anotherSource";
    public static final String URL_VALUE_2 = URL_VALUE + "2";
    public static final String TARGET = "URL";

    private final PIDRecord RECORD = new PIDRecord();

    @BeforeEach()
    void setupRecord() {
        RECORD.addEntry(SOURCE, URL_VALUE);
        RECORD.addEntry(SOURCE_2, URL_VALUE_2);
    }

    @Test
    void oneSourceNoTarget() {
        RecordModifier modifier = new CopyAttribute(Set.of(SOURCE), TARGET);
        assertTrue(RECORD.hasProperty(SOURCE));
        assertFalse(RECORD.hasProperty(TARGET));

        PIDRecord result = modifier.apply(RECORD);
        // Currently, we do unfortunately modify the record because making a proper copy in java for it is difficult.
        assertEquals(RECORD, result);
        assertTrue(result.hasProperty(TARGET));
        assertEquals(URL_VALUE, result.getPropertyValue(TARGET));
        assertEquals(result.getPropertyValue(SOURCE), result.getPropertyValue(TARGET));
    }

    @Test
    void multipleSourceNoTarget() {
        RecordModifier modifier = new CopyAttribute(List.of(SOURCE_2, SOURCE), TARGET);
        assertTrue(RECORD.hasProperty(SOURCE_2));
        assertFalse(RECORD.hasProperty(TARGET));

        PIDRecord result = modifier.apply(RECORD);

        assertTrue(result.hasProperty(TARGET));
        assertEquals(URL_VALUE_2, result.getPropertyValue(TARGET));
        assertEquals(result.getPropertyValue(SOURCE_2), result.getPropertyValue(TARGET));
    }

    @Test
    void oneSourceOneTarget() {
        RECORD.addEntry(TARGET, "whatever");
        assertTrue(RECORD.hasProperty(SOURCE));
        assertTrue(RECORD.hasProperty(TARGET));

        RecordModifier modifier = new CopyAttribute(Set.of(SOURCE), TARGET);
        PIDRecord result = modifier.apply(RECORD);

        assertEquals(URL_VALUE, result.getPropertyValue(SOURCE));
        assertTrue(Arrays.asList(result.getPropertyValues(TARGET)).contains(URL_VALUE));
    }

    @Test
    void noSourceNoTarget() {
        PIDRecord record = new PIDRecord();
        assertFalse(record.hasProperty(SOURCE));
        assertFalse(record.hasProperty(TARGET));

        RecordModifier modifier = new CopyAttribute(Set.of(SOURCE), TARGET);
        PIDRecord result = modifier.apply(record);
        // Shall not modify record
        assertEquals(result, new PIDRecord());
    }
}