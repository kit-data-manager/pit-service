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
class OperationsTest {

    public static final String VALID_DATE = "2021-12-21T17:36:09.541+00:00";
    public static final String TYPE_PROFILE = "21.T11148/076759916209e5d62bd5";

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
    void testFindDateCreatedFailUnregistered() throws IOException {
        PIDRecord pidRecord = new PIDRecord();
        // should fail because the type is not semantically recognizable as a date
        // (because it is not a registered type)
        pidRecord.addEntry("non-registered-creation-date", "", VALID_DATE);
        Optional<Date> date = typingService.getOperations().findDateCreated(pidRecord);
        assertTrue(date.isEmpty());
    }

    @Test
    void testFindDateCreatedFailRegistered() throws IOException {
        PIDRecord pidRecord = new PIDRecord();
        // should fail because the type is not semantically recognizable as a date
        // (although it is a resolveable type)
        pidRecord.addEntry(TYPE_PROFILE, "", "21.T11148/b9b76f887845e32d29f7");
        Optional<Date> date = typingService.getOperations().findDateCreated(pidRecord);
        assertTrue(date.isEmpty());
    }

    @Test
    void testFindDateModified() throws IOException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        Optional<Date> date = typingService.getOperations().findDateModified(pidRecord);
        assertTrue(date.isPresent());
    }

    @Test
    void testFindDateModifiedFailUnregistered() throws IOException {
        PIDRecord pidRecord = new PIDRecord();
        // should fail because the type is not semantically recognizable as a date
        // (because it is not a registered type)
        pidRecord.addEntry("non-registered-creation-date", "", VALID_DATE);
        Optional<Date> date = typingService.getOperations().findDateModified(pidRecord);
        assertTrue(date.isEmpty());
    }

    @Test
    void testFindDateModifiedFailRegistered() throws IOException {
        PIDRecord pidRecord = new PIDRecord();
        // should fail because the type is not semantically recognizable as a date
        // (although it is a resolveable type)
        pidRecord.addEntry(TYPE_PROFILE, "", "21.T11148/b9b76f887845e32d29f7");
        Optional<Date> date = typingService.getOperations().findDateModified(pidRecord);
        assertTrue(date.isEmpty());
    }

    @Test
    void testExtractDate() {
        Optional<Date> date = typingService.getOperations().extractDate(VALID_DATE);
        assertTrue(date.isPresent());
    }

    @Test
    void testExtractDateFail() {
        String dateStr = "asdf";
        Optional<Date> date = typingService.getOperations().extractDate(dateStr);
        assertTrue(date.isEmpty());
    }
}
