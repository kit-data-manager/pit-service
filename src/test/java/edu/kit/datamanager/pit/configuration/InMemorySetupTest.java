package edu.kit.datamanager.pit.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.SpringTestHelper;
import edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;


@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class InMemorySetupTest {

    @Autowired
    ApplicationContext app;

    @Autowired
    private ApplicationProperties props;

    @Test
    @DisplayName("Expecting these tests to use the in-memory configuration.")
    void testDefaultPIDServiceImplementation() {
        IdentifierSystemImpl chosen = this.props.getIdentifierSystemImplementation();
        IdentifierSystemImpl expected = IdentifierSystemImpl.IN_MEMORY;
        assertEquals(chosen, expected);
    }

    @Test
    @DisplayName("Expecting to instantiate in-memory PID service implementation.")
    void testInMemoryPIDServiceInstance() {
        SpringTestHelper helper = new SpringTestHelper(app);
        helper.assertSingleBeanInstanceOf(InMemoryIdentifierSystem.class);
        helper.assertNoBeanInstanceOf(HandleProtocolProperties.class);
        helper.assertNoBeanInstanceOf(HandleProtocolAdapter.class);
    }
}
