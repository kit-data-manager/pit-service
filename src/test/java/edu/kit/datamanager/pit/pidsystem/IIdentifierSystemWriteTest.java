package edu.kit.datamanager.pit.pidsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.RecordTestHelper;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidsystem.impl.local.LocalPidSystem;

/**
 * Write tests are only possible with sandboxed PID systems like InMemory and
 * Local.
 * 
 * The aim is to test the creation of large PID records.
 * 
 * For the Local system, we need the database and therefore a Spring context.
 */
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = "pit.pidsystem.implementation=LOCAL"
)
@ActiveProfiles("test")
class IIdentifierSystemWriteTest {

    @Autowired
    private LocalPidSystem localPidSystem;

    @Autowired
    private PidSuffixGenerator pidGenerator;

    private static final String PID_PREFIX = "sandboxed/";

    @Autowired
    DataSourceProperties dataSourceProperties;
    
    @BeforeEach
    void setup() throws InterruptedException, IOException {
        // ensure we run on an in-memory DB for testing
        //assertTrue(dataSourceProperties.determineUrl().contains("mem"));
        // ensure wiring worked
        assertNotNull(localPidSystem);
        
        // Register just a small record for the database to initialize maybe or so.
        String attribute = pidGenerator.generate().getWithPrefix(PID_PREFIX);
        String value = pidGenerator.generate().getWithPrefix(PID_PREFIX);
        PIDRecord r = new PIDRecord()
                .withPID(pidGenerator.generate().get())
                .addEntry(attribute, "test", value);

        localPidSystem.registerPID(r);
    }

    @Test
    @DisplayName("Testing PID Records with usual/larger size, with the Local PID system (in-memory db).")
    void testExtensiveRecordWithLocalPidSystem() throws IOException {
        // as we use an in-memory db for testing, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;

        PIDRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, PID_PREFIX, pidGenerator);
        assertEquals(numAttributes, r.getPropertyIdentifiers().size());
        assertEquals(numValues, r.getPropertyValues(r.getPropertyIdentifiers().iterator().next()).size());

        this.localPidSystem.registerPID(r);
    }
}
