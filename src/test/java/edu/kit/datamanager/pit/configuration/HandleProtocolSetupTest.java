package edu.kit.datamanager.pit.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.SpringTestHelper;
import edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;

@SpringBootTest(
    properties = {
        //"pit.pidsystem.handle-protocol.credentials.user-handle = fakeUserHandle/forTesting",
        //"pit.pidsystem.handle-protocol.credentials.private-key-path = key.file",
        //"pit.pidsystem.handle-protocol.credentials.private-key-index = 123",
        //"pit.pidsystem.handle-protocol.credentials.handle-identifier-prefix = testing",
    }
)
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = {
        "pit.pidsystem.implementation = HANDLE_PROTOCOL",
    //   "handleProtocolPrivateKeyPassphrase=test123"
    }
)
@ActiveProfiles("test")
class HandleProtocolSetupTest {

    @Autowired
    private ApplicationContext app;

    @Autowired
    private ApplicationProperties props;

    @Autowired
    private HandleProtocolProperties handleProps;
    
    @Test
    @DisplayName("Expecting these tests to use the HANDLE_PROTOCOL configuration.")
    void testConfiguration() {
        assertNotNull(props);
        IdentifierSystemImpl chosen = props.getIdentifierSystemImplementation();
        IdentifierSystemImpl expected = IdentifierSystemImpl.HANDLE_PROTOCOL;
        assertEquals(chosen, expected);

        assertNotNull(handleProps);
        //assertTrue(handleProps.getCredentials() != null);
        //HandleCredentials auth = handleProps.getCredentials();
        //assertEquals(auth.getPrivateKeyIndex(), 123);
        //assertEquals(auth.getPrivateKeyPath(), Path.of("folder/key.file"));
        //assertEquals(auth.getUserHandle(), "fakeUserHandle/forTesting");
        //assertEquals(auth.getPrivateKeyPassphrase(), Optional.of("test123"));
    }

    @Test
    @DisplayName("Expecting to instantiate HANDLE_PROTOCOL PID service implementation.")
    void testInstances() {
        SpringTestHelper helper = new SpringTestHelper(app);
        helper.assertSingleBeanInstanceOf(HandleProtocolProperties.class);
        helper.assertSingleBeanInstanceOf(HandleProtocolAdapter.class);
        helper.assertNoBeanInstanceOf(InMemoryIdentifierSystem.class);
    }
}
