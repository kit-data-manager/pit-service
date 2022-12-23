package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SimplePidRecordTest {
    @Test
    void testContentTypeConstant() {
        assertTrue(SimplePidRecord.CONTENT_TYPE.contains(SimplePidRecord.CONTENT_TYPE_PURE));
    }
}
