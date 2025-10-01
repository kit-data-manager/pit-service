package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JacksonException;

import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.web.ApiMockUtils;

class PidDatabaseObjectTest {
    @Test
    void testConversion() throws JacksonException {
        PidRecord original = ApiMockUtils.getSomePidRecordInstance();
        PidDatabaseObject dbo = new PidDatabaseObject(original);
        PidRecord equivalent = new PidRecord(dbo);

        assertEquals(original, equivalent);
        original.addEntry("test", "", "test");
        assertNotEquals(original, equivalent);
    }
}
