package edu.kit.datamanager.pit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.pit.domain.PIDRecord;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.SpringTestHelper;
import edu.kit.datamanager.pit.pitservice.impl.NoValidationStrategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Might be needed for WebApp testing according to https://www.baeldung.com/integration-testing-in-spring
//@WebAppConfiguration
// Default preparation foo for mockMVC
@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class ITypingRestResourceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.webApplicationContext)
                .build();
        // Make sure validation tests are really validating
        new SpringTestHelper(webApplicationContext)
                .assertNoBeanInstanceOf(NoValidationStrategy.class);
    }

    @Test
    void testPidRecordInErrorJson() throws Exception {
        PIDRecord r = new PIDRecord();
        r.addEntry("21.T11148/076759916209e5d62bd5", "for Testing", "21.T11148/301c6f04763a16f0f72a");
        MvcResult result = this.mockMvc
                .perform(
                        post("/api/v1/pit/pid/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8")
                                .content(new ObjectMapper().writeValueAsString(r))
                                .accept(MediaType.ALL)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pid-record", Matchers.containsString("for Testing")))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().isEmpty());
    }

    /**
     * Tests if the swagger ui and openapi definition is accessible.
     * 
     * Note that this test is using mockMVC; it does probably not detect issues with
     * CSRF, but will recognize other kinds of internal issues.
     * 
     * @throws Exception
     */
    @Test
    void getOpenApiDefinition() throws Exception {
        this.mockMvc.perform(get("/swagger-ui.html"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        this.mockMvc.perform(get("/swagger-ui/index.html"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        this.mockMvc.perform(get("/v3/api-docs"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        assertTrue(true);
    }
}
