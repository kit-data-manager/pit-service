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
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.springframework.context.ConfigurableApplicationContext;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;

public class CliTaskWriteFile implements ICliTask {

    Stream<String> pids;
    ConfigurableApplicationContext context;
    ApplicationProperties appProps;

    public CliTaskWriteFile(ConfigurableApplicationContext context, Stream<String> pids) {
        this.pids = pids;
        this.context = context;
        this.appProps = context.getBeansOfType(ApplicationProperties.class)
            .entrySet()
            .stream()
            .filter(e -> !e.getKey().contains("typing"))
            .map(Entry<String, ApplicationProperties>::getValue)
            .findFirst()
            .orElseThrow();
    }

    @Override
    public void process() throws IOException {
        String date = ZonedDateTime
                .now(ZoneId.systemDefault())
                .toString()
                //.replace(":", ".")
                .replace("[", "(")
                .replace("]", ")")
                .replace("/", "-")
                //.replace("+", "_")
                ;
        String filename = String.format("%s.%s", date, "csv");
        Path path = Paths.get(filename);
        {
            File f = path.toFile();
            f.createNewFile();
        }
        for (Iterator<String> iter = pids.iterator(); iter.hasNext(); ) {
            String pid = iter.next();
            Files.writeString(path, pid + "\n", StandardOpenOption.APPEND);
        }
    }
}
