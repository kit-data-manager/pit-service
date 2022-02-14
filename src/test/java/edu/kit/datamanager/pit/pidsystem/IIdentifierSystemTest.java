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

import edu.kit.datamanager.pit.configuration.HandleProtocolProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;
import net.handle.hdllib.HandleException;

/**
 * This test ensures the interface is implemented correctly, with the following limits:
 * - TODO do write tests (update, register, delete PIDs), as this requires authentication or mocking
 * - TODO do queryByType tests (requires PID with registered type as property)
 */
public class IIdentifierSystemTest {

    static Stream<Arguments> implProvider() throws HandleException, IOException {
        HandleProtocolProperties props = new HandleProtocolProperties();
        props.setCredentials(null);
        HandleProtocolAdapter handleProtocolInstance = new HandleProtocolAdapter(props);
        handleProtocolInstance.init();
        IIdentifierSystem handleProtocol = handleProtocolInstance;

        IIdentifierSystem inMemory = new InMemoryIdentifierSystem();
        PIDRecord inMemoryPidRecord = new PIDRecord();
        inMemoryPidRecord.addEntry(
            // this is actually a registered type, but not in a data type registry, but inline in the PID record.
            "10320/loc",
            "",
            "<locations>\n<location href=\"http://dtr-test.pidconsortium.eu/objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\" />\n"
                + "<location href=\"http://dtr-test.pidconsortium.eu/#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\" />\n"
                + "</locations>"
        );
        String inMemoryPID = inMemory.registerPID(inMemoryPidRecord);

        // TODO initiate REST impl

        return Stream.of(
            Arguments.of(handleProtocol, "21.T11148/076759916209e5d62bd5", "21.T11148/NONEXISTENT123"),
            Arguments.of(inMemory, inMemoryPID, "sandboxed/NONEXISTENT")
        );
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void isIdentifierRegisteredTrue(IIdentifierSystem impl, String pid) throws IOException {
        assertTrue(impl.isIdentifierRegistered(pid));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void isIdentifierRegisteredFalse(IIdentifierSystem impl, String pid, String pid_nonexist) throws IOException {
        assertFalse(impl.isIdentifierRegistered(pid_nonexist));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void queryAllPropertiesExample(IIdentifierSystem impl, String pid) throws IOException {
        PIDRecord result = impl.queryAllProperties(pid);
        assertEquals(result.getPid(), pid);
        assertTrue(result.getPropertyIdentifiers().contains("10320/loc"));
        assertFalse(result.getPropertyIdentifiers().contains("HS_ADMIN"));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void queryAllPropertiesOfNonexistent(IIdentifierSystem impl, String _pid, String pid_nonexist) throws IOException {
        PIDRecord result = impl.queryAllProperties(pid_nonexist);
        assertNull(result);
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void querySingleProperty(IIdentifierSystem impl, String pid) throws IOException {
        TypeDefinition type = new TypeDefinition();
        type.setIdentifier("10320/loc");
        type.setDescription("FakeType for testing. Actually describing the location in some handle specific format, and no registered type");
        String property = impl.queryProperty(pid, type);
        assertTrue(property.contains("<location href=\"http://dtr-test.pidconsortium.eu/objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\" />"));
        assertTrue(property.contains("<location href=\"http://dtr-test.pidconsortium.eu/#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\" />"));
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void queryNonexistentProperty(IIdentifierSystem impl, String pid) throws IOException {
        TypeDefinition type = new TypeDefinition();
        type.setIdentifier("Nonexistent_Property");
        type.setDescription("FakeType for testing. Does not exist and query should fail somehow.");
        String property = impl.queryProperty(pid, type);
        assertNull(property);
    }

    @ParameterizedTest
    @MethodSource("implProvider")
    void queryPropertyOfNonexistent(IIdentifierSystem impl, String pid, String pid_nonexist) throws IOException {
        assertThrows(IOException.class, () -> {
            TypeDefinition type = new TypeDefinition();
            type.setIdentifier("Nonexistent_Property");
            type.setDescription("FakeType for testing. Does not exist and query should fail somehow.");
            impl.queryProperty(pid_nonexist, type);
        });
    }
}
