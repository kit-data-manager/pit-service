package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.xservlet.ServletContext;

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
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


/**
 * This is a dedicated test for the validation/dryrun parameters, available for the REST interface.
 * 
 * It ensures that:
 * - validation is being executed
 * - no data is stored
 * 
 * It uses the in-memory implementation for simplicity.
 * 
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
public class ExplicitValidationParametersTest {

    static final String EMPTY_RECORD = "{\"pid\": null, \"entries\": {}}";
    static final String RECORD = "{\"entries\":{\"21.T11148/076759916209e5d62bd5\":[{\"key\":\"21.T11148/076759916209e5d62bd5\",\"name\":\"kernelInformationProfile\",\"value\":\"21.T11148/301c6f04763a16f0f72a\"}],\"21.T11148/397d831aa3a9d18eb52c\":[{\"key\":\"21.T11148/397d831aa3a9d18eb52c\",\"name\":\"dateModified\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/8074aed799118ac263ad\":[{\"key\":\"21.T11148/8074aed799118ac263ad\",\"name\":\"digitalObjectPolicy\",\"value\":\"21.T11148/37d0f4689c6ea3301787\"}],\"21.T11148/92e200311a56800b3e47\":[{\"key\":\"21.T11148/92e200311a56800b3e47\",\"name\":\"etag\",\"value\":\"{ \\\"sha256sum\\\": \\\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\\\" }\"}],\"21.T11148/aafd5fb4c7222e2d950a\":[{\"key\":\"21.T11148/aafd5fb4c7222e2d950a\",\"name\":\"dateCreated\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/b8457812905b83046284\":[{\"key\":\"21.T11148/b8457812905b83046284\",\"name\":\"digitalObjectLocation\",\"value\":\"https://test.repo/file001\"}],\"21.T11148/c692273deb2772da307f\":[{\"key\":\"21.T11148/c692273deb2772da307f\",\"name\":\"version\",\"value\":\"1.0.0\"}],\"21.T11148/c83481d4bf467110e7c9\":[{\"key\":\"21.T11148/c83481d4bf467110e7c9\",\"name\":\"digitalObjectType\",\"value\":\"21.T11148/ManuscriptPage\"}]},\"pid\":\"unregistered-18622\"}";
    
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PidSuffixGenerator pidGenerator;

    @Autowired
    ITypingService typingService;

    @Autowired
    private ApplicationProperties appProps;

    private MockMvc mockMvc;

    @Autowired
    private KnownPidsDao knownPidsDao;

    @Autowired
    private InMemoryIdentifierSystem inMemory;
    
    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.knownPidsDao.deleteAll();
        this.typingService.setValidationStrategy(this.appProps.defaultValidationStrategy());
    }

    @Test
    public void checkTestSetup() {
        assertNotNull(this.mockMvc);
        assertNotNull(this.webApplicationContext);
        assertNotNull(this.inMemory);
        ServletContext servletContext = webApplicationContext.getServletContext();
        assertNotNull(servletContext);
        assertTrue(servletContext instanceof MockServletContext);
        
        SpringTestHelper springTestHelper = new SpringTestHelper(webApplicationContext);
        springTestHelper.assertSingleBeanInstanceOf(ITypingRestResource.class);
        springTestHelper.assertSingleBeanInstanceOf(InMemoryIdentifierSystem.class);
        springTestHelper.assertNoBeanInstanceOf(HandleProtocolAdapter.class);
    }

    @Test
    public void testCreateEmptyRecord() throws Exception {
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
    public void testExtensiveRecordDryRun() throws Exception {
        // create mockup of a large record. It contains non-registered PIDs and can not be validated.
        this.typingService.setValidationStrategy(new NoValidationStrategy());
        // as we use an in-memory data structure, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;
        assertTrue(numAttributes * numValues > 256);
        PIDRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, "sandboxed/", pidGenerator);
        
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
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk()); // instead of created (201)
        
        // no PIDs are stored with dryrun
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    @DisplayName("Testing PID Records with usual/larger size, without validation. Should return 201, as dryrun is false.")
    public void testExtensiveRecordWithoutDryRun() throws Exception {
        // create mockup of a large record. It contains non-registered PIDs and can not be validated.
        this.typingService.setValidationStrategy(new NoValidationStrategy());
        // as we use an in-memory data structure, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;
        assertTrue(numAttributes * numValues > 256);
        PIDRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, "sandboxed/", pidGenerator);
        
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
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isCreated()); // instead of created (201)
        
        // dryrun was false, so there should be a new PID known
        assertEquals(1, this.knownPidsDao.count());
    }

    @Test
    public void testNontypeRecord() throws Exception {
        PIDRecord r = new PIDRecord();
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
    public void testInvalidRecordWithProfile() throws Exception {
        PIDRecord r = new PIDRecord();
        r.addEntry("21.T11148/076759916209e5d62bd5", "for Testing", "21.T11148/301c6f04763a16f0f72a");
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
    @DisplayName("Resolve a PID known to be valid, with explicit validation.")
    public void testResolvingValidRecordWithValidation() throws Exception {
        this.testExtensiveRecordWithoutDryRun();
        assertEquals(1, knownPidsDao.count());
        String validPid = knownPidsDao.findAll().iterator().next().getPid();
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
    public void testResolvingValidRecordWithValidationFail() throws Exception {
        // note: this test disables validation...
        this.testExtensiveRecordWithoutDryRun();
        assertEquals(1, knownPidsDao.count());
        String validPid = knownPidsDao.findAll().iterator().next().getPid();

        PIDRecord r = inMemory.queryAllProperties(validPid);
        r.addEntry("21.T11148/076759916209e5d62bd5", "", "21.T11148/b9b76f887845e32d29f7");
        r.addEntry("something wrong", "", "someVeryUniqueValue");
        inMemory.updatePID(r);
        // ... so we need to re-enable validation here:
        this.typingService.setValidationStrategy(this.appProps.defaultValidationStrategy());

        MvcResult result = this.mockMvc
            .perform(
                get("/api/v1/pit/pid/" + validPid)
                    .param("validation", "true")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();
        assertTrue(0 < result.getResponse().getErrorMessage().length());
    }
}
