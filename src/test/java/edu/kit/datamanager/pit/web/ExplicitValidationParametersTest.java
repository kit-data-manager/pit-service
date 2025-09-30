package edu.kit.datamanager.pit.web;

import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import jakarta.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.RecordTestHelper;
import edu.kit.datamanager.pit.SpringTestHelper;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pidsystem.impl.handle.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.impl.NoValidationStrategy;

// org.springframework.mock is for unit testing
// Source: https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html
import org.springframework.mock.web.MockServletContext;

// org.springframework.test is for integration testing (dependency "spring-test")
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * This is a dedicated test for the validation/dryrun parameters, available for the REST interface.
 * <p>
 * It ensures that:
 * - validation is being executed
 * - no data is stored
 * <p>
 * It uses the in-memory implementation for simplicity.
 * <p>
 * Explicit validation parameters are:
 * - dryrun=true for creating a PID
 * - validation=true for resolving a PID
 */
// Default preparation foo for mockMVC
@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class ExplicitValidationParametersTest {

    static final String EMPTY_RECORD = "{\"pid\": null, \"entries\": {}}";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PidSuffixGenerator pidGenerator;

    @Autowired
    ITypingService typingService;

    @Autowired
    ITypeRegistry typeRegistry;

    @Autowired
    private ApplicationProperties appProps;

    private MockMvc mockMvc;

    @Autowired
    private KnownPidsDao knownPidsDao;

    @Autowired
    private InMemoryIdentifierSystem inMemory;
    
    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.knownPidsDao.deleteAll();
        this.typingService.setValidationStrategy(
                this.appProps.defaultValidationStrategy(typeRegistry));
    }

    @Test
    void checkTestSetup() {
        assertNotNull(this.mockMvc);
        assertNotNull(this.webApplicationContext);
        assertNotNull(this.inMemory);
        ServletContext servletContext = webApplicationContext.getServletContext();
        assertNotNull(servletContext);
        assertInstanceOf(MockServletContext.class, servletContext);
        
        SpringTestHelper springTestHelper = new SpringTestHelper(webApplicationContext);
        springTestHelper.assertSingleBeanInstanceOf(ITypingRestResource.class);
        springTestHelper.assertSingleBeanInstanceOf(InMemoryIdentifierSystem.class);
        springTestHelper.assertNoBeanInstanceOf(HandleProtocolAdapter.class);
    }

    @Test
    void testCreateEmptyRecord() throws Exception {
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .param("dryrun", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(EMPTY_RECORD)
                    .accept(MediaType.ALL)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
        
        // we store PIDs only if the PID was created successfully
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    @DisplayName("Testing PID Records with usual/larger size, without validation. Should return 200 instead of 201 with dryrun.")
    void testExtensiveRecordDryRun() throws Exception {
        // create mockup of a large record. It contains non-registered PIDs and can not be validated.
        this.typingService.setValidationStrategy(new NoValidationStrategy());
        // as we use an in-memory data structure, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;
        PidRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, "sandboxed/", pidGenerator);
        
        String rJson = ApiMockUtils.serialize(r);
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .param("dryrun", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(rJson)
                    .accept(MediaType.ALL)
            )
            //.andDo(MockMvcResultHandlers.print()) // output is massive due to the large record
            .andExpect(MockMvcResultMatchers.status().isOk()); // instead of created (201)
        
        // no PIDs are stored with dryrun
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    @DisplayName("Testing PID Records with usual/larger size, without validation. Should return 201, as dryrun is false.")
    void testExtensiveRecordWithoutDryRun() throws Exception {
        // create mockup of a large record. It contains non-registered PIDs and can not be validated.
        this.typingService.setValidationStrategy(new NoValidationStrategy());
        // as we use an in-memory data structure, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;
        PidRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, "sandboxed/", pidGenerator);
        
        String rJson = ApiMockUtils.serialize(r);
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .param("dryrun", "false")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(rJson)
                    .accept(MediaType.ALL)
            )
            //.andDo(MockMvcResultHandlers.print()) // output is massive due to the large record
            .andExpect(MockMvcResultMatchers.status().isCreated()); // instead of created (201)

        // dryrun was false, so there should be a new PID known
        assertEquals(1, this.knownPidsDao.count());
    }

    @Test
    void testNontypeRecord() throws Exception {
        PidRecord r = new PidRecord();
        r.addEntry("unregisteredType", "for Testing", "hello");
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .param("dryrun", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(new ObjectMapper().writeValueAsString(r))
                    .accept(MediaType.ALL)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
        
        // we store PIDs only if the PID was created successfully
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    void testRecordWithInvalidValue() throws Exception {
        PidRecord r = new PidRecord();
        // valid attribute key, but wrong attribute value:
        String urlType = "21.T11969/e0efc41346cda4ba84ca";
        r.addEntry(urlType, "", "not a url");
        this.mockMvc
                .perform(
                        post("/api/v1/pit/pid/")
                                .param("dryrun", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8")
                                .content(new ObjectMapper().writeValueAsString(r))
                                .accept(MediaType.ALL)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.detail",
                        Matchers.containsString("has a non-complying value")));

        // we store PIDs only if the PID was created successfully
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    void testRecordWithAdditionalAttribute() throws Exception {
        PidRecord r = ApiMockUtils.getSomePidRecordInstance();
        r.addEntry(
                "21.T11969/86963861a2b249a83b93",
                "additional attribute",
                "{\"image-context-name\": \"itsa'me!\", \"image-context-uri\": \"https://example.com/mario\"}");
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .param("dryrun", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(new ObjectMapper().writeValueAsString(r))
                    .accept(MediaType.ALL)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk());

        // we store PIDs only if the PID was created (no dryrun)
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    @DisplayName("Resolve a PID known to be valid, with explicit validation.")
    void testResolvingValidRecordWithValidation() throws Exception {
        this.testExtensiveRecordWithoutDryRun();
        assertEquals(1, knownPidsDao.count());
        String validPid = knownPidsDao.findAll().getFirst().getPid();
        this.mockMvc
            .perform(
                get("/api/v1/pit/pid/" + validPid)
                    .param("validation", "true")
                    .accept(SimplePidRecord.CONTENT_TYPE)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Resolve a PID known to be invalid, with explicit validation.")
    void testResolvingInvalidRecordWithValidationFail() throws Exception {
        // We'll reuse the extensive record here, and validate it.
        // To do so, we resolve the PID, set validate to true, and expect a validation error.
        // This error must occur, as all attributes are made up. These PIDs are not registered
        // and were generated using this.pidGenerator.

        // note: this test disables validation...
        this.testExtensiveRecordWithoutDryRun();
        assertEquals(1, knownPidsDao.count());
        String validPid = knownPidsDao.findAll().getFirst().getPid();
        // ... so we need to re-enable validation here:
        this.typingService.setValidationStrategy(
                this.appProps.defaultValidationStrategy(typeRegistry));
        // Now, we can resolve and validate:
        MvcResult result = this.mockMvc
            .perform(
                get("/api/v1/pit/pid/" + validPid)
                    .param("validation", "true")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.containsString("Type not found")))
            .andReturn();
        assertFalse(result.getResponse().getContentAsString().isEmpty());
    }
}
