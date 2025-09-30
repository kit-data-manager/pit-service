package edu.kit.datamanager.pit.pidsystem.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.domain.PidRecord;

class InMemoryIdentifierSystemTest {

    private InMemoryIdentifierSystem sys;

    @BeforeEach
    void setup() {
        this.sys = new InMemoryIdentifierSystem();
    }

    @Test
    void testDeletePid() throws IOException {
        PidRecord p = new PidRecord().withPID("test/pid");
        sys.registerPid(p);
        String pid = p.getPid();
        assertThrows(
            UnsupportedOperationException.class,
            () -> sys.deletePid(pid)
        );

        // actually, this is the case for any PID:
        assertThrows(
            UnsupportedOperationException.class,
            () -> sys.deletePid("any PID")
        );
    }

    @Test
    void testResolveAll() throws InvalidConfigException, IOException {
        assertEquals(0, sys.resolveAllPidsOfPrefix().size());

        PidRecord p1 = new PidRecord().withPID("p1");
        sys.registerPid(p1);
        assertEquals(1, sys.resolveAllPidsOfPrefix().size());

        PidRecord p2 = new PidRecord().withPID("p2");
        sys.registerPid(p2);
        assertEquals(2, sys.resolveAllPidsOfPrefix().size());
    }

}
