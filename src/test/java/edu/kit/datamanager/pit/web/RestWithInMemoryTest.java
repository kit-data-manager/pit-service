package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pidsystem.impl.HandleProtocolAdapter;
import edu.kit.datamanager.pit.pidsystem.impl.InMemoryIdentifierSystem;
import edu.kit.datamanager.pit.web.impl.TypingRESTResourceImpl;

// org.springframework.mock is for unit testing
// Source: https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html
import org.springframework.mock.web.MockServletContext;

// org.springframework.test is for integration testing (dependency "spring-test")
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


// Might be needed for WebApp testing according to https://www.baeldung.com/integration-testing-in-spring
//@WebAppConfiguration
// Default preparation foo for mockMVC
@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
// TODO why a testing profile?
@ActiveProfiles("test")
public class RestWithInMemoryTest {

    static final String EMPTY_RECORD = "{\"pid\": null, \"entries\": {}}";
    static final String RECORD = "{\"entries\":{\"21.T11148/076759916209e5d62bd5\":[{\"key\":\"21.T11148/076759916209e5d62bd5\",\"name\":\"kernelInformationProfile\",\"value\":\"21.T11148/301c6f04763a16f0f72a\"}],\"21.T11148/397d831aa3a9d18eb52c\":[{\"key\":\"21.T11148/397d831aa3a9d18eb52c\",\"name\":\"dateModified\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/8074aed799118ac263ad\":[{\"key\":\"21.T11148/8074aed799118ac263ad\",\"name\":\"digitalObjectPolicy\",\"value\":\"21.T11148/37d0f4689c6ea3301787\"}],\"21.T11148/92e200311a56800b3e47\":[{\"key\":\"21.T11148/92e200311a56800b3e47\",\"name\":\"etag\",\"value\":\"{ \\\"sha256sum\\\": \\\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\\\" }\"}],\"21.T11148/aafd5fb4c7222e2d950a\":[{\"key\":\"21.T11148/aafd5fb4c7222e2d950a\",\"name\":\"dateCreated\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/b8457812905b83046284\":[{\"key\":\"21.T11148/b8457812905b83046284\",\"name\":\"digitalObjectLocation\",\"value\":\"https://test.repo/file001\"}],\"21.T11148/c692273deb2772da307f\":[{\"key\":\"21.T11148/c692273deb2772da307f\",\"name\":\"version\",\"value\":\"1.0.0\"}],\"21.T11148/c83481d4bf467110e7c9\":[{\"key\":\"21.T11148/c83481d4bf467110e7c9\",\"name\":\"digitalObjectType\",\"value\":\"21.T11148/ManuscriptPage\"}]},\"pid\":\"unregistered-18622\"}";
    
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private ObjectMapper mapper;

    @Autowired
    private KnownPidsDao knownPidsDao;

    @Autowired
    private TypingRESTResourceImpl restImpl;

    private static final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant YESTERDAY = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    
    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        this.mapper = this.webApplicationContext.getBean("OBJECT_MAPPER_BEAN", ObjectMapper.class);
        this.knownPidsDao.deleteAll();
    }

    @Test
    public void checkTestSetup() {
        assertNotNull(this.mockMvc);
        assertNotNull(this.webApplicationContext);
        ServletContext servletContext = webApplicationContext.getServletContext();
        
        assertNotNull(servletContext);
        assertTrue(servletContext instanceof MockServletContext);
        assertNotNull(webApplicationContext.getBean(ITypingRestResource.class));
        assertNotNull(webApplicationContext.getBean(InMemoryIdentifierSystem.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            webApplicationContext.getBean(HandleProtocolAdapter.class);
        });
        assertEquals("false", this.webApplicationContext.getEnvironment().getProperty("repo.messaging.enabled"));
    }

    @Test
    public void testNotFound() throws Exception {
        this.mockMvc.perform(
                get("/api/v1/pit/pid/prefix/pid")
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
        ;
    }

    @Test
    public void testCreateEmptyRecord() throws Exception {
        this.mockMvc
            .perform(
                post("/api/v1/pit/pid/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(EMPTY_RECORD)
                    .accept(MediaType.ALL)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isConflict());
        // we store PIDs only if the PID was created successfully
        assertEquals(0, this.knownPidsDao.count());

        // Nothing is registered, our local storags contains no PIDs
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    public void testCreateValidRecord() throws Exception {
        // test create
        PIDRecord createdRecord = this.createSomeRecord();
        String createdPid = createdRecord.getPid();

        // We store created PIDs
        assertEquals(1, this.knownPidsDao.count());

        // test resolve
        PIDRecord resolvedRecord = resolveRecord(createdPid);
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
    public void testUpdateRecord() throws Exception {
        PIDRecord record = this.createSomeRecord();
        record.getEntries().get("21.T11148/b8457812905b83046284").get(0).setValue("https://example.com/anotherUrlAsBefore");
        PIDRecord updatedRecord = this.updateRecord(record);
        assertEquals(record, updatedRecord);
    }

    @Test
    public void testIdPidRegisteredFails() throws Exception {
        // Nothing is registered, our local storags contains no PIDs
        PIDRecord nonRegistered = mapper.readValue(RECORD, PIDRecord.class);
        boolean isRegistered = isPidRegistered(nonRegistered.getPid());
        assertFalse(isRegistered);
        assertEquals(0, this.knownPidsDao.count());
    }

    @Test
    public void testIsPidRecordRegisteredSucceeds() throws Exception {
        PIDRecord existing = this.createSomeRecord();
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
        PIDRecord r = this.createSomeRecord();
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
        PIDRecord r = this.createSomeRecord();
        PIDRecord r2 = this.createSomeRecord();
        assertEquals(2, this.knownPidsDao.count());
        assertNotEquals(r.getPid(), r2.getPid());
        
        List<KnownPid> pidinfos = queryKnownPIDs(YESTERDAY, TOMORROW, null, null);
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());
        
        pidinfos = queryKnownPIDs(null, TOMORROW, null, null);
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = queryKnownPIDs(YESTERDAY, null, null, null);
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = queryKnownPIDs(null, null, null, TOMORROW);
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = queryKnownPIDs(null, null, YESTERDAY, null);
        
        assertEquals(2, pidinfos.size());
        assertEquals(r.getPid(), pidinfos.get(0).getPid());
        assertEquals(r2.getPid(), pidinfos.get(1).getPid());

        pidinfos = queryKnownPIDs(null, NOW.minusSeconds(60), null, null);
        assertEquals(0, pidinfos.size());
    }

    /**
     * Wrapper to query known PIDs via API given time intervals for the creation
     * timestamp and modification timestamp. This is a reusable etst component.
     * 
     * @param createdAfter   lower end for the creation timestamp interval
     * @param createdBefore  upper end for the creation timestamp interval
     * @param modifiedAfter  lower end for the modification timestamp interval
     * @param modifiedBefore upper end for the modification timestamp interval
     * @return the result of the query
     * @throws Exception on failed assumptions
     */
    private List<KnownPid> queryKnownPIDs(Instant createdAfter, Instant createdBefore, Instant modifiedAfter, Instant modifiedBefore)
            throws Exception {
        MockHttpServletRequestBuilder request =  get("/api/v1/pit/known-pid/");
        if (createdAfter != null) {
            request.param("created_after", String.valueOf(createdAfter));
        }
        if (createdBefore != null) {
            request.param("created_before", String.valueOf(createdBefore));
        }
        if (modifiedAfter != null) {
            request.param("modified_after", String.valueOf(modifiedAfter));
        }
        if (modifiedBefore != null) {
            request.param("modified_before", String.valueOf(modifiedBefore));
        }
        MvcResult result = this.mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        List<KnownPid> pidinfos = Arrays.asList(this.mapper.readerForArrayOf(KnownPid.class).readValue(body));
        return pidinfos;
    }

    /**
     * The same as `queryKnownPIDs()` but instead of mocking the REST API, it is
     * directly calling the function. Can be used to debug tests. This is a reusable
     * test component.
     * 
     * @param createdAfter   lower end for the creation timestamp interval
     * @param createdBefore  upper end for the creation timestamp interval
     * @param modifiedAfter  lower end for the modification timestamp interval
     * @param modifiedBefore upper end for the modification timestamp interval
     * @return the result of the query
     * @throws Exception on failed assumptions
     */
    private List<KnownPid> queryKnownPIDsViaJava(Instant createdAfter, Instant createdBefore, Instant modifiedAfter, Instant modifiedBefore) throws IOException {
        ResponseEntity<Collection<KnownPid>> response = this.restImpl.findByInterval(createdAfter, createdBefore, modifiedAfter, modifiedBefore, Pageable.ofSize(10), null, null, null);
        Collection<KnownPid> pidinfos = response.getBody();
        return new ArrayList<>(pidinfos);
    }

    /**
     * Updates a PID record and makes some generic tests. This is a reusable test
     * component.
     * 
     * @param record the record, containing the information as it should be after
     *               the update.
     * @return the record as it is after the update.
     * @throws Exception if any assumption breaks. Do not catch, let your test fail
     *                   if this happens.
     */
    PIDRecord updateRecord(PIDRecord record) throws Exception {
        assertFalse(record.getPid().isEmpty());
        MvcResult updated = this.mockMvc
                .perform(
                    put("/api/v1/pit/pid/" + record.getPid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(mapper.writeValueAsString(record))
                        .accept(MediaType.ALL)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
            
        String body = updated.getResponse().getContentAsString();
        PIDRecord updatedRecord = mapper.readValue(body, PIDRecord.class);

        // Lets make generic checks about the local store, which remembers PIDs.
        KnownPid kp = this.knownPidsDao.findByPid(updatedRecord.getPid()).get();
        // The created time and the modified time is the same after creation.
        assertTrue(kp.getCreated().isBefore(kp.getModified()));
        return updatedRecord;
    }

    /**
     * Resolves a record and does make some generic tests. This is a reusable test
     * component.
     * 
     * @param createdPid
     * @return the resolved record of the given PID.
     * @throws Exception if any assumption breaks. Do not catch, let your test fail
     *                   if this happens.
     */
    private PIDRecord resolveRecord(String createdPid) throws Exception {
        MvcResult resolved = this.mockMvc
            .perform(
                get("/api/v1/pit/pid/".concat(createdPid))
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        
        String resolvedBody = resolved.getResponse().getContentAsString();
        PIDRecord resolvedRecord = mapper.readValue(resolvedBody, PIDRecord.class);

        return resolvedRecord;
    }

    /**
     * Creates a record and does make some generic tests. This is a reusable test
     * component.
     * 
     * @return The created PID record.
     * @throws Exception if any assumption breaks. Do not catch, let your test fail
     *                   if this happens.
     */
    PIDRecord createSomeRecord() throws Exception {
        MvcResult created = this.mockMvc
                .perform(
                    post("/api/v1/pit/pid/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(RECORD)
                        .accept(MediaType.ALL)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
            
        String createdBody = created.getResponse().getContentAsString();
        PIDRecord createdRecord = mapper.readValue(createdBody, PIDRecord.class);
        String createdPid = createdRecord.getPid();
        assertTrue(createdPid.contains("sandboxed/"));

        // Lets make generic checks about the local store, which remembers PIDs.
        KnownPid kp = this.knownPidsDao.findByPid(createdRecord.getPid()).get();
        // The created time and the modified time is the same after creation.
        assertEquals(kp.getCreated(), kp.getModified());
        return createdRecord;
    }
}
