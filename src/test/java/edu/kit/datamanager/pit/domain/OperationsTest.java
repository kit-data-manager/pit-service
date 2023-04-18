package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.web.ApiMockUtils;

// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class OperationsTest {
    @Test
    void testFindDateCreated() throws IOException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        Optional<Date> date = Operations.findDateCreated(pidRecord);
        assertTrue(date.isPresent());
    }

    @Test
    void testFindDateModified() throws IOException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        Optional<Date> date = Operations.findDateModified(pidRecord);
        assertTrue(date.isPresent());
    }

    @Test
    void testExtractDate() {
        String dateStr = "2021-12-21T17:36:09.541+00:00";
        Optional<Date> date = Operations.extractDate(dateStr);
        assertTrue(date.isPresent());
    }
}
