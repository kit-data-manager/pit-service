package edu.kit.datamanager.pit.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;

// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties"
)
@ActiveProfiles("test")
public class CliTaskBootstrapTest {

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    KnownPidsDao knownPids;

    @Test
    void setup() {
        assertNotNull(context);
        assertNotNull(knownPids);
    }

    @Test
    void testBootstrapFromPrefix() throws IOException {
        Stream<String> pidSource = PidSource.fromPrefix(context);
        ICliTask task = new CliTaskBootstrap(context, pidSource);
        boolean shutdown = task.process();
        assertFalse(shutdown);
    }

    @Test
    void testBootstrapFromKnown() throws IOException {
        Stream<String> pidSource = PidSource.fromKnown(context);
        ICliTask task = new CliTaskBootstrap(context, pidSource);
        boolean shutdown = task.process();
        assertFalse(shutdown);
    }

    @Test
    void testBootstrapFromCustom() throws IOException {
        Stream<String> pidSource = Stream.of("pid1", "pid2");
        ICliTask task = new CliTaskBootstrap(context, pidSource);
        boolean shutdown = task.process();
        assertFalse(shutdown);
    }

    @Test
    void testNoOverride() throws InvalidConfigException, IOException {
        // lets say a pid exists and has dates assigned
        String pid = "some/pid";
        Instant date = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        knownPids.save(new KnownPid(pid, date, date));
        // the bootstrap task is not allowed to remove/change date information
        Stream<String> pidSource = Stream.of(pid);
        ICliTask task = new CliTaskBootstrap(context, pidSource);
        task.process();
        // nothing should have changed for the pid
        Optional<KnownPid> known = knownPids.findByPid(pid);
        assertTrue(known.isPresent());
        assertEquals(date, known.get().getCreated());
        assertEquals(date, known.get().getModified());
        knownPids.deleteById(pid);
    }

    @AfterEach
    void cleanup() {
        try {
            knownPids.deleteById("pid1");
            knownPids.deleteById("pid2");
        } catch (Exception e) {
            // ignore, as they do not exist in every test
        }
    }
}
