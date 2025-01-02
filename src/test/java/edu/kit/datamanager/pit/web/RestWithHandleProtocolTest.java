package edu.kit.datamanager.pit.web;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

// Might be needed for WebApp testing according to
// https://www.baeldung.com/integration-testing-in-spring
// @WebAppConfiguration
// Default preparation foo for mockMVC
@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest(properties = {
    // Set the Handle Protocol implementation
    "pit.pidsystem.implementation = HANDLE_PROTOCOL"
})
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
/**
 * Testing with the handle protocol is currently read-only.
 * This is due to the fact that we would need credentials for testing.
 * We can test internal processes, though, for example using the dryrun APIs.
 */
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
    void testUpdateWithPidGiven() throws Exception {
        String etag = this.mockMvc.perform(
            get("/api/v1/pit/pid/21.11152/474a4b1c-de93-4d4a-b33d-1d32d63baf4b?validation=false")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
                .getResponse()
                .getHeader("ETag");
        this.mockMvc.perform(
            put("/api/v1/pit/pid/21.11152/474a4b1c-de93-4d4a-b33d-1d32d63baf4b?dryrun=true")
                .content("{ \"pid\": \"21.11152/474a4b1c-de93-4d4a-b33d-1d32d63baf4b\", \"entries\": { \"21.T11148/076759916209e5d62bd5\": [ { \"key\": \"21.T11148/076759916209e5d62bd5\", \"name\": \"kernelInformationProfile\", \"value\": \"21.T11148/b9b76f887845e32d29f7\" } ], \"21.T11148/397d831aa3a9d18eb52c\": [ { \"key\": \"21.T11148/397d831aa3a9d18eb52c\", \"name\": \"dateModified\", \"value\": \"2024-10-14T07:16:46+00:00\" } ], \"21.T11148/82e2503c49209e987740\": [ { \"key\": \"21.T11148/82e2503c49209e987740\", \"name\": \"checksum\", \"value\": \"{ \\\"sha256sum\\\": \\\"a92ad3bd2b0856b70d3f98cb2fa21964ea7f91218c46e327b65a0937c50a885c\\\" }\" } ], \"21.T11148/aafd5fb4c7222e2d950a\": [ { \"key\": \"21.T11148/aafd5fb4c7222e2d950a\", \"name\": \"dateCreated\", \"value\": \"2024-10-14T07:16:46+00:00\" } ], \"21.T11148/b8457812905b83046284\": [ { \"key\": \"21.T11148/b8457812905b83046284\", \"name\": \"digitalObjectLocation\", \"value\": \"https://paint-database.org/WRI1030197/WRI1030197-catalog-stac.json\" } ], \"21.T11148/1a73af9e7ae00182733b\": [ { \"key\": \"21.T11148/1a73af9e7ae00182733b\", \"name\": \"contact\", \"value\": \"https://orcid.org/0009-0007-0235-4995\" }, { \"key\": \"21.T11148/1a73af9e7ae00182733b\", \"name\": \"contact\", \"value\": \"https://orcid.org/0000-0002-2233-1041\" }, { \"key\": \"21.T11148/1a73af9e7ae00182733b\", \"name\": \"contact\", \"value\": \"https://orcid.org/0000-0001-9648-4385\" }, { \"key\": \"21.T11148/1a73af9e7ae00182733b\", \"name\": \"contact\", \"value\": \"https://orcid.org/0000-0002-9197-1739\" }, { \"key\": \"21.T11148/1a73af9e7ae00182733b\", \"name\": \"contact\", \"value\": \"https://orcid.org/0000-0002-4705-6285\" } ], \"21.T11148/2f314c8fe5fb6a0063a8\": [ { \"key\": \"21.T11148/2f314c8fe5fb6a0063a8\", \"name\": \"licenseURL\", \"value\": \"https://cdla.dev/permissive-2-0/\" } ], \"21.T11148/1c699a5d1b4ad3ba4956\": [ { \"key\": \"21.T11148/1c699a5d1b4ad3ba4956\", \"name\": \"digitalResourceType\", \"value\": \"application/json\" } ] }}")
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .header("If-Match", etag)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }
}
