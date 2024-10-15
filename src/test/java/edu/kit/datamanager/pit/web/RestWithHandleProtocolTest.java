package edu.kit.datamanager.pit.web;

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
// TODO why a testing profile?
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
        assertEquals(pid, resolvedRecord.pid());
    }
}
