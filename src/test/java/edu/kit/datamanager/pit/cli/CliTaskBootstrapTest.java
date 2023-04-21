package edu.kit.datamanager.pit.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
