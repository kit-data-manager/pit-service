package edu.kit.datamanager.pit.pitservice.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import edu.kit.datamanager.pit.SpringTestHelper;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;

// JUnit5 + Spring
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class TypingServiceTest {

    @Autowired
    private ApplicationContext app;

    @Autowired
    private TypingService typing;

    @Test
    void defaultStrategyIsBeingAssigned() {
        // There must be exactly one Strategy Bean in our application context:
        new SpringTestHelper(app)
                .assertSingleBeanInstanceOf(IValidationStrategy.class);
        // and if it does, it should be autowired with our TypingService:
        assertNotNull(typing.defaultStrategy);
    }
}
