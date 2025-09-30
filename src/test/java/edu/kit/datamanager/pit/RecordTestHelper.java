package edu.kit.datamanager.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;

import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

public class RecordTestHelper {
    /**
     * Generates a PID record with N attibutes, each having M values. All attributes
     * and values are PIDs of {@link pidGenerator} prefixed with {@link PID_PREFIX}.
     * 
     * @param numAttributes the number of attibutes (called N in the description
     *                      above)
     * @param numValues     the number of attributes (called M in the description
     *                      above)
     * @return a PID record as configured.
     */
    public static PidRecord getFakePidRecord(
            final int numAttributes,
            final int numValues,
            final String prefix,
            final PidSuffixGenerator generator)
    {
        PidRecord r = new PidRecord();
        r.setPid(generator.generate().get());
        IntStream.range(0, numAttributes)
                .mapToObj(i -> generator.generate().getWithPrefix(prefix))
                .forEach(attribute -> {
                    IntStream.range(0, numValues)
                            .mapToObj(i -> generator.generate().getWithPrefix(prefix))
                            .forEach(value -> r.addEntry(attribute, "name", value));
                });

        // In theory these could fail if the generator generated a PID twice, but it is very unlikely.
        assertEquals(numAttributes, r.getPropertyIdentifiers().size());
        assertEquals(numValues, r.getPropertyValues(r.getPropertyIdentifiers().iterator().next()).length);
        
        return r;
    }
}
