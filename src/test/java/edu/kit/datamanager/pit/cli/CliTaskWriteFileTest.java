package edu.kit.datamanager.pit.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class CliTaskWriteFileTest {
    @Test
    void testCreateFilename() {
        Stream<String> pids = Stream.of("pid1", "pid2");
        CliTaskWriteFile task = new CliTaskWriteFile(pids);
        String filename = task.createFilename();
        // Checks for the current OS for the validity of this path.
        // Note that our CI runs tests on different operating systems.
        assertDoesNotThrow(() -> Paths.get(filename));
        // The last check does not consider file length, though.
        // So lets make sure the filename is not too long:
        assertTrue(filename.length() < 100);
    }
}
