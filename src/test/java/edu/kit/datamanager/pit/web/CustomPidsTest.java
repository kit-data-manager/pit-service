package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import edu.kit.datamanager.pit.domain.PIDRecord;

@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class CustomPidsTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    /**
     * Test: Register a PID which is already registered.
     * Expect: HTTP 409 (conflict)
     */
    @Test
    void testResolvePid() throws Exception {
        PIDRecord first = ApiMockUtils.registerSomeRecord(this.mockMvc);
        assertNotNull(first);

        PIDRecord second = ApiMockUtils.getSomePidRecordInstance();
        second.setPid(first.getPid());
        String body = ApiMockUtils.getJsonMapper().writeValueAsString(second);
        
        ApiMockUtils.registerRecord(
            mockMvc,
            body,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isConflict()
        );
    }
}
