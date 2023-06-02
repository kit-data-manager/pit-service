package edu.kit.datamanager.pit.pidgeneration.generators;

import java.util.UUID;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

/**
 * Generates a PID suffix based on a UUID4.
 */
public class PidSuffixGenUuid4 implements PidSuffixGenerator {

    @Override
    public PidSuffix generate() {
        String uuid = UUID.randomUUID().toString();
        return new PidSuffix(uuid);
    }
}
