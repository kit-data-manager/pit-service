package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.web.ApiMockUtils;

// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class OperationsTest {

    @Autowired
    private ITypingService typingService;

    @Test
    void testSpringSetup() {
        assertNotNull(typingService);
    }

    @Test
    void testFindDateCreated() throws IOException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        Optional<Date> date = typingService.getOperations().findDateCreated(pidRecord);
        assertTrue(date.isPresent());
    }

    @Test
    void testFindDateModified() throws IOException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        Optional<Date> date = typingService.getOperations().findDateModified(pidRecord);
        assertTrue(date.isPresent());
    }

    @Test
    void testExtractDate() {
        String dateStr = "2021-12-21T17:36:09.541+00:00";
        Optional<Date> date = typingService.getOperations().extractDate(dateStr);
        assertTrue(date.isPresent());
    }

    @Test
    void testExtractDateFail() {
        String dateStr = "asdf";
        Optional<Date> date = typingService.getOperations().extractDate(dateStr);
        assertTrue(date.isEmpty());
    }
}
