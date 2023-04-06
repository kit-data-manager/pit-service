package edu.kit.datamanager.pit.cli;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.springframework.context.ConfigurableApplicationContext;

import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

/**
 * All static methods are sources for PIDs. Used in Command Line Tasks.
 */
public class PidSource {

    private PidSource() {
        // This class contains only static methods and shall not be instanciated.
    }

    public static Stream<String> fromPrefix(ConfigurableApplicationContext context) throws IOException {
        return context
                .getBeansOfType(IIdentifierSystem.class)
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().contains("typing"))
                .map(Entry<String, IIdentifierSystem>::getValue)
                .findFirst()
                .orElseThrow()
                .resolveAllPidsOfPrefix()
                .stream();
    }

    public static Stream<String> fromKnown(ConfigurableApplicationContext context) {
        return context
                .getBean(KnownPidsDao.class)
                .findAll()
                .stream()
                .map(e -> e.getPid());
    }

}
