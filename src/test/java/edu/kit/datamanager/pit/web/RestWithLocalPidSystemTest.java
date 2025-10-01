package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import jakarta.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.RecordTestHelper;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pidsystem.impl.handle.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;
import edu.kit.datamanager.pit.pidsystem.impl.local.LocalPidSystem;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


// Might be needed for WebApp testing according to https://www.baeldung.com/integration-testing-in-spring
//@WebAppConfiguration
// Default preparation foo for mockMVC
@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = "pit.pidsystem.implementation = LOCAL"
)
@ActiveProfiles("test")
class RestWithLocalPidSystemTest {

    static final String EMPTY_RECORD = "{\"pid\": null, \"entries\": {}}";
    static final String RECORD = "{\"entries\":{\"21.T11148/076759916209e5d62bd5\":[{\"key\":\"21.T11148/076759916209e5d62bd5\",\"name\":\"kernelInformationProfile\",\"value\":\"21.T11148/301c6f04763a16f0f72a\"}],\"21.T11148/397d831aa3a9d18eb52c\":[{\"key\":\"21.T11148/397d831aa3a9d18eb52c\",\"name\":\"dateModified\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/8074aed799118ac263ad\":[{\"key\":\"21.T11148/8074aed799118ac263ad\",\"name\":\"digitalObjectPolicy\",\"value\":\"21.T11148/37d0f4689c6ea3301787\"}],\"21.T11148/92e200311a56800b3e47\":[{\"key\":\"21.T11148/92e200311a56800b3e47\",\"name\":\"etag\",\"value\":\"{ \\\"sha256sum\\\": \\\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\\\" }\"}],\"21.T11148/aafd5fb4c7222e2d950a\":[{\"key\":\"21.T11148/aafd5fb4c7222e2d950a\",\"name\":\"dateCreated\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/b8457812905b83046284\":[{\"key\":\"21.T11148/b8457812905b83046284\",\"name\":\"digitalObjectLocation\",\"value\":\"https://test.repo/file001\"}],\"21.T11148/c692273deb2772da307f\":[{\"key\":\"21.T11148/c692273deb2772da307f\",\"name\":\"version\",\"value\":\"1.0.0\"}],\"21.T11148/c83481d4bf467110e7c9\":[{\"key\":\"21.T11148/c83481d4bf467110e7c9\",\"name\":\"digitalObjectType\",\"value\":\"21.T11148/ManuscriptPage\"}]},\"pid\":\"unregistered-18622\"}";
    
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PidSuffixGenerator pidGenerator;

    @Autowired
    ITypingService typingService;

    @Autowired
    ITypeRegistry typeRegistry;

    @Autowired
    ApplicationProperties appProps;

    private MockMvc mockMvc;

    private ObjectMapper mapper;

    @Autowired
    private KnownPidsDao knownPidsDao;

    private static final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant YESTERDAY = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    
    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.mapper = this.webApplicationContext.getBean("OBJECT_MAPPER_BEAN", ObjectMapper.class);
        this.knownPidsDao.deleteAll();
        this.typingService.setValidationStrategy(
                this.appProps.defaultValidationStrategy(typeRegistry));
    }

    @Test
    void checkTestSetup() {
        assertNotNull(this.mockMvc);
        assertNotNull(this.webApplicationContext);
        ServletContext servletContext = webApplicationContext.getServletContext();
        
        assertNotNull(servletContext);
        assertTrue(servletContext instanceof MockServletContext);
        assertNotNull(webApplicationContext.getBean(ITypingRestResource.class));
        assertNotNull(webApplicationContext.getBean(LocalPidSystem.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            webApplicationContext.getBean(HandleProtocolAdapter.class);
        });
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            webApplicationContext.getBean(InMemoryIdentifierSystem.class);
        });
        assertEquals("false", this.webApplicationContext.getEnvironment().getProperty("repo.messaging.enabled"));
    }

    @Test
    void testNotFound() throws Exception {
        this.mockMvc.perform(
                get("/api/v1/pit/pid/prefix/pid")
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
        ;
    }

    @Test
    void testCreateEmptyRecord() throws Exception {
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(EMPTY_RECORD)
                    .accept(MediaType.ALL)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
        // we store PIDs only if the PID was created successfully
        assertEquals(0, this.knownPidsDao.count());

        // Nothing is registered, our local storags contains no PIDs
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    void testCreateValidRecord() throws Exception {
        // test create
        PidRecord createdRecord = ApiMockUtils.registerSomeRecord(this.mockMvc);
        String createdPid = createdRecord.getPid();

        // We store created PIDs
        assertEquals(1, this.knownPidsDao.count());

        // test resolve
        PidRecord resolvedRecord = ApiMockUtils.resolveRecord(this.mockMvc, createdPid);
        assertEquals(createdRecord, resolvedRecord);

        // Resolving the PID will override the available entry.
        assertEquals(1, this.knownPidsDao.count());
        KnownPid kp = this.knownPidsDao.findByPid(resolvedRecord.getPid()).get();
        // After resolving, the PID is not modified. So the creation and modification timestamps are still the same.
        assertEquals(kp.getCreated(), kp.getModified());
        // on update only
        kp.getCreated().isBefore(kp.getModified());
    }

    @Test
    @DisplayName("Testing PID Records with usual/larger size, with the Local PID system (in-memory db).")
    void testExtensiveRecord() throws Exception {
        // create mockup of a large record. It contains non-registered PIDs and can not be validated.
        this.typingService.setValidationStrategy(new NoValidationStrategy());
        // as we use an in-memory db for testing, lets not make it too large.
        int numAttributes = 100;
        int numValues = 100;
        assertTrue(numAttributes * numValues > 256);
        PidRecord r = RecordTestHelper.getFakePidRecord(numAttributes, numValues, "sandboxed/", pidGenerator);
        
        String rJson = ApiMockUtils.serialize(r);
        ApiMockUtils.registerRecord(
            this.mockMvc,
            rJson,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MockMvcResultMatchers.status().isCreated()
        );
    }

    @Test
    void testUpdateRecord() throws Exception {
        PidRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        PidRecord modified = ApiMockUtils.clone(original);
        modified.getEntries().get("21.T11148/b8457812905b83046284").get(0).setValue("https://example.com/anotherUrlAsBefore");
        assertNotEquals(original, modified);
        PidRecord updatedRecord = ApiMockUtils.updateRecord(this.mockMvc, original, modified);
        assertEquals(modified, updatedRecord);
    }

    @Test
    void testIdPidRegisteredFails() throws Exception {
        // Nothing is registered, our local storags contains no PIDs
        PidRecord nonRegistered = mapper.readValue(RECORD, PidRecord.class);
        boolean isRegistered = isPidRegistered(nonRegistered.getPid());
        assertFalse(isRegistered);
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    void testIsPidRecordRegisteredSucceeds() throws Exception {
        PidRecord existing = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // We know a PID after we create one.
        assertEquals(1, this.knownPidsDao.count());
        // If we clear the locally stored PIDs and then ask if it is registered, it should appear again.
        this.knownPidsDao.deleteAll();
        assertEquals(0, this.knownPidsDao.count());
        boolean isRegistered = isPidRegistered(existing.getPid());
        assertTrue(isRegistered);
        assertEquals(1, this.knownPidsDao.count());
    }

    private boolean isPidRegistered(String pid) throws Exception {
        MvcResult result = this.mockMvc
                .perform(
                    head("/api/v1/pit/pid/" + pid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .accept(MediaType.ALL)
                )
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        return result.getResponse().getStatus() == 200;
    }

    @Test
    void testKnownPidFailure() throws Exception {
        this.mockMvc.perform(get("/api/v1/pit/known-pid/fake/pid"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void testKnownPidSuccess() throws Exception {
        PidRecord r = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // we know it is in the local database:
        assertEquals(1, this.knownPidsDao.count());
        // so we should be able to retrieve it via the REST api:
        this.mockMvc.perform(get("/api/v1/pit/known-pid/" + r.getPid()))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void testKnownPidIntervalFailure() throws Exception {
        this.mockMvc.perform(get("/api/v1/pit/known-pid/fake/pid").header("created_after", Instant.now()))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * This test ensures that the different combinations of parameters can be given
     * to this endpoint.
     * 
     * @throws Exception on failed assumptions
     */
    @Test
    void testKnownPidIntervalSuccessWithCreatedInterval() throws Exception {
        PidRecord r = ApiMockUtils.registerSomeRecord(this.mockMvc);
        PidRecord r2 = ApiMockUtils.registerSomeRecord(this.mockMvc);
        assertEquals(2, this.knownPidsDao.count());
        assertNotEquals(r.getPid(), r2.getPid());
        
        List<KnownPid> pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, YESTERDAY, TOMORROW, null, null, Optional.empty());
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());
        
        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, null, TOMORROW, null, null, Optional.empty());
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, YESTERDAY, null, null, null, Optional.empty());
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, null, null, null, TOMORROW, Optional.empty());
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, null, null, YESTERDAY, null, Optional.empty());
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, null, NOW.minusSeconds(60), null, null, Optional.empty());
        assertEquals(0, pidinfos.size());
    }

    /**
     * This test documents the way pagination can be used with the API. If this test
     * fails/changes, it likely must be released as a new major release.
     * 
     * @throws Exception on failed assumptions
     */
    @Test
    void testKnownPidIntervalWithPaging() throws Exception {
        // see also https://www.baeldung.com/rest-api-pagination-in-spring
        PidRecord r = ApiMockUtils.registerSomeRecord(this.mockMvc);
        PidRecord r2 = ApiMockUtils.registerSomeRecord(this.mockMvc);
        assertEquals(2, this.knownPidsDao.count());
        assertNotEquals(r.getPid(), r2.getPid());
        List<KnownPid> pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, YESTERDAY, TOMORROW, null, null, Optional.empty());
        assertEquals(2, pidinfos.size());

        Pageable pageable = Pageable.ofSize(1).first();
        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, YESTERDAY, TOMORROW, null, null, Optional.of(pageable));
        assertEquals(1, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());

        pageable = pageable.next();
        pidinfos = ApiMockUtils.queryKnownPIDs(this.mockMvc, YESTERDAY, TOMORROW, null, null, Optional.of(pageable));
        assertEquals(1, pidinfos.size());
        assertEquals(r2.getPid(), pidinfos.get(0).getPid());
    }
}
