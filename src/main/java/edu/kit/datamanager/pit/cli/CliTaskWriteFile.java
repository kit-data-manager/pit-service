package edu.kit.datamanager.pit.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliTaskWriteFile implements ICliTask {

    private static final Logger LOG = LoggerFactory.getLogger(CliTaskWriteFile.class);

    protected Stream<String> pids;

    public CliTaskWriteFile(Stream<String> pids) {
        this.pids = pids;
    }

    @Override
    public boolean process() throws IOException {
        String filename = createFilename();
        
        Path path = Paths.get(filename);
        ensureFileExists(path);

        writeToFile(path);
        return true;
    }

    protected void writeToFile(Path path) throws IOException {
        for (Iterator<String> iter = pids.iterator(); iter.hasNext(); ) {
            String pid = iter.next();
            LOG.info("Storing into CSV: {}", pid);
            Files.writeString(path, pid + "\n", StandardOpenOption.APPEND);
        }
    }

    protected void ensureFileExists(Path path) throws IOException {
        File f = path.toFile();
        f.createNewFile();
    }

    protected String createFilename() {
        String date = ZonedDateTime
                .now(ZoneId.systemDefault())
                .toString()
                //.replace(":", ".")
                .replace("[", "(")
                .replace("]", ")")
                .replace("/", "-")
                //.replace("+", "_")
                ;
        return String.format("%s.%s", date, "csv");
    }
}
