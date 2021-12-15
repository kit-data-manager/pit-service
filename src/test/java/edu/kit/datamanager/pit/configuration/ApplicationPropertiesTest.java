package edu.kit.datamanager.pit.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class ApplicationPropertiesTest {

    @Autowired
    private ApplicationProperties props;

    @Test
    @DisplayName("Testing default PID Service Implemenation Configuration Value to be IN_MEMORY.")
    void testDefaultPIDServiceImplementation() {
        IdentifierSystemImpl chosen = this.props.getIdentifierSystemImplementation();
        IdentifierSystemImpl expected = IdentifierSystemImpl.IN_MEMORY;
        assertEquals(chosen, expected);
    }
}
