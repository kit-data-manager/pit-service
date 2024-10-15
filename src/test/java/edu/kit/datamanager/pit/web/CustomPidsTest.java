package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.configuration.PidGenerationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;

@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = {"pit.pidgeneration.custom-client-pids-enabled = true"}
)
@ActiveProfiles("test")
class CustomPidsTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    PidGenerationProperties props;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.props.setCustomClientPidsEnabled(true);
    }

    /**
     * Test: Register a PID which is already registered.
     * Expect: HTTP 409 (conflict)
     */
    @Test
    void testCreateExistingPid() throws Exception {
        PIDRecord first = ApiMockUtils.registerSomeRecord(this.mockMvc);
        assertNotNull(first);

        PIDRecord second = ApiMockUtils.getSomePidRecordInstance();
        second = second.withPID(first.pid());
        String body = ApiMockUtils.getJsonMapper().writeValueAsString(second);
        
        ApiMockUtils.registerRecord(
            mockMvc,
            body,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isConflict()
        );
    }

    /**
     * Test: Register a PID with a custom PID.
     * Expect: HTTP 201 (created)
     */
    @Test
    void testCreateCustomPid() throws Exception {
        PIDRecord record = ApiMockUtils.getSomePidRecordInstance();
        record = record.withPID("my-custom-pid");
        String body = ApiMockUtils.getJsonMapper().writeValueAsString(record);
        
        String responseBody = ApiMockUtils.registerRecord(
            mockMvc,
            body,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isCreated()
        );
        assertTrue(responseBody.contains(record.pid()));
    }

    /**
     * Test: Register a PID with a custom PID when the feature is actually disabled.
     * Expect: HTTP 201 (created) but PID is not the same (input ignored)
     */
    @Test
    void testCrateCustomPidWhenFeatureDisabled() throws Exception {
        this.props.setCustomClientPidsEnabled(false);
        String customPid = "my-custom-pid";

        PIDRecord record = ApiMockUtils.getSomePidRecordInstance();
        record = record.withPID(customPid);
        String body = ApiMockUtils.getJsonMapper().writeValueAsString(record);
        
        String responseBody = ApiMockUtils.registerRecord(
            mockMvc,
            body,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isCreated()
        );
        assertFalse(responseBody.contains(customPid));
    }

    /**
     * Test: Register a PID with a branding-prefix being set.
     * Expect: HTTP 201 (created) and PID does not contain branding-prefix.
     * 
     * We assume that this dangerous custom PID feature gives full control to the client.
     */
    @Test
    void testBrandingNotApplied() throws Exception {
        String branding = "test-branding.";
        this.props.setBrandingPrefix(Optional.of(branding));
        String customPid = "unbranded-pid";

        PIDRecord record = ApiMockUtils.getSomePidRecordInstance();
        record = record.withPID(customPid);
        String body = ApiMockUtils.getJsonMapper().writeValueAsString(record);
        
        String responseBody = ApiMockUtils.registerRecord(
            mockMvc,
            body,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isCreated()
        );
        assertTrue(responseBody.contains(customPid));
        assertFalse(responseBody.contains(branding));
    }
}
