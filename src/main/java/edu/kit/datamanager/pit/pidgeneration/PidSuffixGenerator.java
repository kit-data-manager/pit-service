package edu.kit.datamanager.pit.pidgeneration;

import java.util.stream.Stream;

/**
 * Enables the implementor to generate a PID suffix.
 * 
 * Note that a {@link PidSuffixGenerator} might consume another
 * {@link PidSuffixGenerator} to build a suffix from their ourput.
 */
public interface PidSuffixGenerator {
    /**
     * Generate a PID suffix.
     */
    public abstract PidSuffix generate();

    /**
     * Generates an infinite stream of PID suffixes, given the
     * {@link PidSuffixGenerator#generate()} implementation.
     * 
     * Note that you have to end such a stream with a terminal operation like
     * {@link Stream#limit(long)} or {@link Stream#findFirst()}. If you do not do
     * this, but use {@link Stream#forEach(java.util.function.Consumer)}, you will
     * get an infinite loop!
     * 
     * The idea is to use {@link Stream#filter(java.util.function.Predicate)} to
     * filter out unwanted suffixes and then call {@link Stream#findFirst()}.
     * 
     * @return an infinite stream of PID suffixes.
     */
    public default Stream<PidSuffix> inifiteStream() {
        return Stream.generate(this::generate);
    }
}
