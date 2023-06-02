package edu.kit.datamanager.pit.pidgeneration.generators;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

/**
 * Generates hex chunks of four characters each, separated by a dash.
 * 
 * Corresponds to four chunks by default, offering more than enough PIDs.
 */
public class HexChunksGenerator implements PidSuffixGenerator {

    protected Random random = new SecureRandom();

    protected int numChunks = 4;

    public HexChunksGenerator(int numChunks) {
        this.numChunks = numChunks;
    }

    @Override
    public PidSuffix generate() {
        String suffix = IntStream.range(0, numChunks)
                .mapToObj(i -> this.generateChunk())
                .collect(Collectors.joining("-"));

        return new PidSuffix(suffix);
    }

    private String generateChunk() {
        return IntStream.range(0, 4)
                .mapToObj(i -> Integer.toHexString(random.nextInt(16)))
                .collect(Collectors.joining())
                .toUpperCase();

    }

}
