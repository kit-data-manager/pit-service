package edu.kit.datamanager.pit.web;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import jakarta.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testing with the handle protocol is currently read-only.
 * This is due to the fact that we would need credentials for testing.
 * We can test internal processes, though, for example using the dryrun APIs.
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    // Set the Handle Protocol implementation
    "pit.pidsystem.implementation = HANDLE_PROTOCOL"
})
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class RestWithHandleProtocolTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private ObjectMapper mapper;

    static final String pid = "21.T11148/076759916209e5d62bd5";

    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.mapper = this.webApplicationContext.getBean("OBJECT_MAPPER_BEAN", ObjectMapper.class);
    }

    @Test
    void checkTestSetup() {
        assertNotNull(this.mockMvc);
        assertNotNull(this.webApplicationContext);
        ServletContext servletContext = webApplicationContext.getServletContext();

        assertNotNull(servletContext);
        assertInstanceOf(MockServletContext.class, servletContext);
        assertNotNull(webApplicationContext.getBean(ITypingRestResource.class));
        assertNotNull(webApplicationContext.getBean(HandleProtocolAdapter.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            webApplicationContext.getBean(InMemoryIdentifierSystem.class);
        });
    }

    @Test
    void resolveSomething() throws Exception {
        MvcResult resolved = this.mockMvc
            .perform(
                get("/api/v1/pit/pid/".concat(pid))
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        String resolvedBody = resolved.getResponse().getContentAsString();
        PIDRecord resolvedRecord = mapper.readValue(resolvedBody, PIDRecord.class);
        assertEquals(pid, resolvedRecord.getPid());
    }

    @Test
    void testDryrunUpdateWithPidGiven() throws Exception {
        String url = "/api/v1/pit/pid/21.11152/474a4b1c-de93-4d4a-b33d-1d32d63baf4b";
        MockHttpServletResponse response = this.mockMvc.perform(
            get(url).param("validation", "false")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse();
        String etag = response.getHeader("ETag");
        PIDRecord record = mapper.readValue(response.getContentAsString(), PIDRecord.class);
        // fix record, it is actually invalid...
        record.removeAllValuesOf("URL");
        // fix possible issue with this type in current state of type api
        record.removeAllValuesOf("21.T11148/2f314c8fe5fb6a0063a8");
        String licenseUrl = "21.T11969/e0efc41346cda4ba84ca";
        record.removeAllValuesOf(licenseUrl);
        record.addEntry(licenseUrl, "https://cdla.dev/permissive-2-0/");
        this.mockMvc.perform(
            put(url)
                .param("dryrun", "true")
                .content(mapper.writeValueAsString(record))
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .header("If-Match", etag)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }
}
