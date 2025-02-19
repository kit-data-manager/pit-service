package edu.kit.datamanager.pit.resolver;

import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class ResolverTest {

    @Autowired
    ITypingService identifierSystem;

    Resolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new Resolver(this.identifierSystem);
    }

    @Test
    void resolveWithoutPrefix() {
        assertThrows(PidNotFoundException.class, () -> resolver.resolve("test"));
    }

    @Test
    void resolveWithNonexistentPrefix() {
        assertThrows(PidNotFoundException.class, () -> resolver.resolve("nonexistentprefix/test"));
    }

    @Test
    void resolveHandleReadOnly() {
        PIDRecord result = resolver.resolve("10.5281/zenodo.8014937");
        assertNotNull(result);
    }

    @Test
    void resolveInMemory() {
        PIDRecord record = new PIDRecord().withPID("suffix");
        record.addEntry("key", "value");
        String pid = this.identifierSystem.registerPid(record);
        PIDRecord result = resolver.resolve(pid);
        assertNotNull(result);
        assertEquals("value", result.getEntries().get("key").getFirst().getValue());
    }
}