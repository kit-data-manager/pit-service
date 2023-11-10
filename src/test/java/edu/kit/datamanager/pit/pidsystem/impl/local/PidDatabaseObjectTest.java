package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JacksonException;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.web.ApiMockUtils;

class PidDatabaseObjectTest {
    @Test
    void testConversion() throws JacksonException {
        PIDRecord original = ApiMockUtils.getSomePidRecordInstance();
        PidDatabaseObject dbo = new PidDatabaseObject(original);
        PIDRecord equivalent = new PIDRecord(dbo);

        assertEquals(original, equivalent);
        original.addEntry("test", "", "test");
        assertNotEquals(original, equivalent);
    }
}
