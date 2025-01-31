package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystemQueryTest;

/**
 * This tests the same things as `IIdentifierSystemTest`, but is separated from
 * it as it is not possible to have all the spring bean magic in a static
 * method, which is required to prepare the parameterized test cases.
 */
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = "pit.pidsystem.implementation=LOCAL"
)
@ActiveProfiles("test")
class LocalPidSystemTest {
    IIdentifierSystemQueryTest systemTests = new IIdentifierSystemQueryTest();
    
    @Autowired
    LocalPidSystem localPidSystem;

    @Autowired
    DataSourceProperties dataSourceProperties;
    
    @BeforeEach
    void setup() throws InterruptedException, IOException {
        // ensure we run on an in-memory DB for testing
        assertTrue(dataSourceProperties.determineUrl().contains("mem"));
        // ensure wiring worked
        assertNotNull(localPidSystem);
        assertNotNull(localPidSystem.getDatabase());
        // ensure DB is empty
        localPidSystem.getDatabase().deleteAll();
    }
    
    @Test
    @Transactional
    void testAllSystemTests() throws Exception {
        PIDRecord rec = new PIDRecord();
        rec.setPid("my-custom-pid");
        rec.addEntry(
            // this is actually a registered type, but not in a data type registry, but inline in the PID system.
            "10320/loc",
            "",
            "objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\""
            + "#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\""
        );
        //rec.addEntry("10320/loc", "", "value");
        String pid = localPidSystem.registerPid(rec);
        assertEquals(rec.getPid(), pid);
        PIDRecord newRec = localPidSystem.queryPid(pid);
        assertEquals(rec, newRec);
        
        Set<Method> publicMethods = new HashSet<>(Arrays.asList(IIdentifierSystemQueryTest.class.getMethods()));
        Set<Method> allDirectMethods = new HashSet<>(Arrays.asList(IIdentifierSystemQueryTest.class.getDeclaredMethods()));
        publicMethods.retainAll(allDirectMethods);
        assertEquals(6, publicMethods.size());
        for (Method test : publicMethods) {
            int numParams = test.getParameterCount();
            if (numParams == 2) {
                try {
                    test.invoke(systemTests, localPidSystem, rec.getPid());
                } catch (Exception e) {
                    System.err.printf("Test: %s%n", test);
                    System.err.printf("Exception: %s%n", e);
                    throw e;
                }
            } else if (numParams == 3) {
                test.invoke(systemTests, localPidSystem, rec.getPid(), "sandboxed/NONEXISTENT");
            } else if (numParams == 0) {
                // This is not a test but some kind of helper or static method.
            } else {
                throw new Exception("There was a method with an unexpected amount of parameters. Handle this case here.");
            }
        }
    }

    @Test
    void testDeletePid() throws IOException {
        PIDRecord p = new PIDRecord().withPID("test/pid");
        this.localPidSystem.registerPid(p);
        String pid = p.getPid();
        assertThrows(
            UnsupportedOperationException.class,
            () -> this.localPidSystem.deletePid(pid)
        );

        // actually, this is the case for any PID:
        assertThrows(
            UnsupportedOperationException.class,
            () -> this.localPidSystem.deletePid("any PID")
        );
    }

    @Test
    void testResolveAll() throws InvalidConfigException, IOException {
        assertEquals(0, this.localPidSystem.resolveAllPidsOfPrefix().size());

        PIDRecord p1 = new PIDRecord().withPID("p1");
        this.localPidSystem.registerPid(p1);
        assertEquals(1, this.localPidSystem.resolveAllPidsOfPrefix().size());

        PIDRecord p2 = new PIDRecord().withPID("p2");
        this.localPidSystem.registerPid(p2);
        assertEquals(2, this.localPidSystem.resolveAllPidsOfPrefix().size());
    }
}
