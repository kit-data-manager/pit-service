package edu.kit.datamanager.pit.pidsystem.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;

class InMemoryIdentifierSystemTest {

    private InMemoryIdentifierSystem sys;
    private TypeDefinition profile;
    private TypeDefinition t1;
    private TypeDefinition t2;
    private TypeDefinition t3;

    @BeforeEach
    void setup() {
        this.sys = new InMemoryIdentifierSystem();
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
    void testQueryByType() throws IOException {

        PIDRecord p = new PIDRecord().withPID("test/pid");

        // an empty registered record will return nothing
        sys.registerPID(p);
        PIDRecord queried = sys.queryByType(p.pid(), profile);
        assertTrue(queried.getPropertyIdentifiers().isEmpty());

        // a record with matching types will return only those
        p = p.addEntry(t1.getIdentifier(), "noName", "value")
                .addEntry("something else", "noName", "noValue");
        sys.updatePID(p);
        queried = sys.queryByType(p.pid(), profile);
        assertEquals(1, queried.getPropertyIdentifiers().size());
    }

    @Test
    void testDeletePid() throws IOException {
        PIDRecord p = new PIDRecord().withPID("test/pid");
        sys.registerPID(p);
        String pid = p.pid();
        assertThrows(
            UnsupportedOperationException.class,
            () -> sys.deletePID(pid)
        );

        // actually, this is the case for any PID:
        assertThrows(
            UnsupportedOperationException.class,
            () -> sys.deletePID("any PID")
        );
    }

    @Test
    void testResolveAll() throws InvalidConfigException, IOException {
        assertEquals(0, sys.resolveAllPidsOfPrefix().size());

        PIDRecord p1 = new PIDRecord().withPID("p1");
        sys.registerPID(p1);
        assertEquals(1, sys.resolveAllPidsOfPrefix().size());

        PIDRecord p2 = new PIDRecord().withPID("p2");
        sys.registerPID(p2);
        assertEquals(2, sys.resolveAllPidsOfPrefix().size());
    }

}
