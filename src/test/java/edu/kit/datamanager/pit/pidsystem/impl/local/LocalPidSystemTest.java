package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystemTest;

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
public class LocalPidSystemTest {
    IIdentifierSystemTest systemTests = new IIdentifierSystemTest();
    
    @Autowired
    LocalPidSystem localPidSystem;

    @Autowired
    DataSourceProperties dataSourceProperties;

    private TypeDefinition profile;
    private TypeDefinition t1;
    private TypeDefinition t2;
    private TypeDefinition t3;
    
    @BeforeEach
    void setup() throws InterruptedException, IOException {
        // ensure we run on an in-memory DB for testing
        assertTrue(dataSourceProperties.determineUrl().contains("mem"));
        // ensure wiring worked
        assertNotNull(localPidSystem);
        assertNotNull(localPidSystem.getDatabase());
        // ensure DB is empty
        localPidSystem.getDatabase().deleteAll();
        // prepare types and profiles
        this.t1 = new TypeDefinition();
        this.t1.setIdentifier("attribute1");
        this.t2 = new TypeDefinition();
        this.t2.setIdentifier("attribute2");
        this.t3 = new TypeDefinition();
        this.t3.setIdentifier("attribute3");
    
        this.profile = new TypeDefinition();
        this.profile.setSubTypes(Map.of(
            this.t1.getIdentifier(), this.t1,
            this.t2.getIdentifier(), this.t2,
            this.t3.getIdentifier(), this.t3
        ));
    }
    
    @Test
    @Transactional
    void testAllSystemTests() throws Exception {
        PIDRecord rec = new PIDRecord();
        rec.addEntry(
            // this is actually a registered type, but not in a data type registry, but inline in the PID system.
            "10320/loc",
            "",
            "objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\""
            + "#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\""
        );
        //rec.addEntry("10320/loc", "", "value");
        String pid = localPidSystem.registerPID(rec);
        assertEquals(rec.getPid(), pid);
        PIDRecord newRec = localPidSystem.queryAllProperties(pid);
        assertEquals(rec, newRec);
        
        Set<Method> publicMethods = new HashSet<>(Arrays.asList(IIdentifierSystemTest.class.getMethods()));
        Set<Method> allDirectMethods = new HashSet<>(Arrays.asList(IIdentifierSystemTest.class.getDeclaredMethods()));
        publicMethods.retainAll(allDirectMethods);
        assertEquals(7, publicMethods.size());
        for (Method test : publicMethods) {
            int numParams = test.getParameterCount();
            if (numParams == 2) {
                try {
                    test.invoke(systemTests, localPidSystem, rec.getPid());
                } catch (Exception e) {
                    System.err.println(String.format("Test: %s", test));
                    System.err.println(String.format("Exception: %s", e));
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
    void testQueryByType() throws IOException {

        PIDRecord p = new PIDRecord().withPID("test/pid");

        // an empty registered record will return nothing
        this.localPidSystem.registerPID(p);
        PIDRecord queried = this.localPidSystem.queryByType(p.getPid(), profile);
        assertTrue(queried.getPropertyIdentifiers().isEmpty());

        // a record with matching types will return only those
        p.addEntry(t1.getIdentifier(), "noName", "value");
        p.addEntry("something else", "noName", "noValue");
        this.localPidSystem.updatePID(p);
        queried = this.localPidSystem.queryByType(p.getPid(), profile);
        assertEquals(1, queried.getPropertyIdentifiers().size());
    }

    @Test
    void testDeletePid() throws IOException {
        PIDRecord p = new PIDRecord().withPID("test/pid");
        this.localPidSystem.registerPID(p);
        String pid = p.getPid();
        assertThrows(
            UnsupportedOperationException.class,
            () -> this.localPidSystem.deletePID(pid)
        );

        // actually, this is the case for any PID:
        assertThrows(
            UnsupportedOperationException.class,
            () -> this.localPidSystem.deletePID("any PID")
        );
    }

    @Test
    void testResolveAll() throws InvalidConfigException, IOException {
        assertEquals(0, this.localPidSystem.resolveAllPidsOfPrefix().size());

        PIDRecord p1 = new PIDRecord().withPID("p1");
        this.localPidSystem.registerPID(p1);
        assertEquals(1, this.localPidSystem.resolveAllPidsOfPrefix().size());

        PIDRecord p2 = new PIDRecord().withPID("p1");
        this.localPidSystem.registerPID(p2);
        assertEquals(2, this.localPidSystem.resolveAllPidsOfPrefix().size());
    }
}
