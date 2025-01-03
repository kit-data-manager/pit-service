package edu.kit.datamanager.pit.pidsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;
import net.handle.hdllib.HandleException;

/**
 * This test ensures the interface is implemented correctly, with the following
 * limits:
 * 
 * - Only read-tests (write tests are only possible for sandboxed systems on a
 * regular and automated basis)
 * - TODO do queryByType tests (requires PID with registered type as property)
 * 
 * NOTE: The difference to the tests in the web module is that this only tests
 * the pidsystem without any validation or other functionality.
 */
public class IIdentifierSystemQueryTest {

    private static Stream<Arguments> implProvider() throws HandleException, IOException {
        HandleProtocolProperties props = new HandleProtocolProperties();
        props.setCredentials(null);
        HandleProtocolAdapter handleProtocolInstance = new HandleProtocolAdapter(props);
        handleProtocolInstance.init();
        IIdentifierSystem handleProtocol = handleProtocolInstance;

        PIDRecord rec = new PIDRecord();
        rec.setPid("my-custom-suffix");
        rec.addEntry(
            // this is actually a registered type, but not in a data type registry, but inline in the PID system.
            "10320/loc",
            "",
            "<locations>\n<location href=\"http://dtr-test.pidconsortium.eu/objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\" />\n"
            + "<location href=\"http://dtr-test.pidconsortium.eu/#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\" />\n"
            + "</locations>"
        );

        IIdentifierSystem inMemory = new InMemoryIdentifierSystem();
        String inMemoryPid = inMemory.registerPid(rec);

        // TODO initiate REST impl

        return Stream.of(
            Arguments.of(handleProtocol, "21.T11148/076759916209e5d62bd5", "21.T11148/NONEXISTENT123"),
            Arguments.of(inMemory, inMemoryPid, "sandboxed/NONEXISTENT")
        );
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void isPidRegisteredTrue(IIdentifierSystem impl, String pid) throws IOException {
        assertTrue(impl.isPidRegistered(pid));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void isPidRegisteredFalse(IIdentifierSystem impl, String pid, String pid_nonexist) throws IOException {
        assertFalse(impl.isPidRegistered(pid_nonexist));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void queryPidExample(IIdentifierSystem impl, String pid) throws IOException {
        PIDRecord result = impl.queryPid(pid);
        assertEquals(result.getPid(), pid);
        assertTrue(result.getPropertyIdentifiers().contains("10320/loc"));
        assertFalse(result.getPropertyIdentifiers().contains("HS_ADMIN"));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void queryPidOfNonexistent(IIdentifierSystem impl, String _pid, String pid_nonexist) throws IOException {
        assertThrows(PidNotFoundException.class, () -> {
            impl.queryPid(pid_nonexist);
        });
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void querySingleProperty(IIdentifierSystem impl, String pid) throws IOException {
        PIDRecord record = impl.queryPid(pid);
        String attributeKey = "10320/loc";
        assertTrue(record.getPropertyIdentifiers().contains(attributeKey));
        String value = record.getPropertyValue(attributeKey);
        assertTrue(value.contains("objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\""));
        assertTrue(value.contains("#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\""));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    public void queryNonexistentProperty(IIdentifierSystem impl, String pid) throws IOException {
        PIDRecord record = impl.queryPid(pid);
        assertFalse(record.getPropertyIdentifiers().contains("Nonexistent_Attribute"));
    }
}
