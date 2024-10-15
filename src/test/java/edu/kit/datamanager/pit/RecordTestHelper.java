package edu.kit.datamanager.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.PidRecordEntry;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;

public class RecordTestHelper {
    /**
     * Generates a PID record with N attributes, each having M values. All attributes
     * and values are PIDs of {@link pidGenerator} prefixed with {@link PID_PREFIX}.
     * 
     * @param numAttributes the number of attibutes (called N in the description
     *                      above)
     * @param numValues     the number of attributes (called M in the description
     *                      above)
     * @return a PID record as configured.
     */
    public static PIDRecord getFakePidRecord(
            final int numAttributes,
            final int numValues,
            final String prefix,
            final PidSuffixGenerator generator)
    {
        Map<String, List<PidRecordEntry>> entries = IntStream.range(0, numAttributes)
                .mapToObj(i -> generator.generate().getWithPrefix(prefix))
                .flatMap(attribute -> IntStream.range(0, numValues)
                        .mapToObj(i -> generator.generate().getWithPrefix(prefix))
                        .map(value -> new AbstractMap.SimpleEntry<>(
                                attribute,
                                new PidRecordEntry(attribute, "", value))
                        ))
                .collect(Collectors.toMap(
                        AbstractMap.SimpleEntry::getKey,
                        e -> new ArrayList<>(List.of(e.getValue())),
                        (e1, e2) -> {
                            e1.addAll(e2);
                            return e1;
                        }
                ));
        PIDRecord r = new PIDRecord(generator.generate().get(), entries);

        // In theory these could fail if the generator generated a PID twice, but it is very unlikely.
        assertEquals(numAttributes, r.getPropertyIdentifiers().size());
        assertEquals(numValues, r.getPropertyValues(r.getPropertyIdentifiers().iterator().next()).size());
        
        return r;
    }
}
