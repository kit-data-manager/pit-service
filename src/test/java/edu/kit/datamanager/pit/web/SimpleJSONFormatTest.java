package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;

@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class SimpleJSONFormatTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    public void testResolvePid() throws Exception {
        PIDRecord complexFormat = ApiMockUtils.createSomeRecord(this.mockMvc);
        assertNotNull(complexFormat);

        SimplePidRecord simpleFormat = ApiMockUtils.resolveSimpleRecord(this.mockMvc, complexFormat.getPid());
        assertNotNull(simpleFormat);
        assertNotNull(simpleFormat.getPairs());

        assertEquals(complexFormat.getPid(), simpleFormat.getPid());
        long complexPairs = complexFormat.getEntries().values().stream().mapToLong(list -> list.size()).sum();

        System.out.println(simpleFormat.getPairs());
        assertEquals(complexPairs, simpleFormat.getPairs().size());
    }

    /**
     * Input: simple json
     * Accept: simple json
     * Expect: simple json, HTTP 201
     */
    @Test
    public void testCreatePidFromSimpleAndAcceptSimple() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
    }

    /**
     * Input: simple json
     * Accept: any
     * Expect: simple json, HTTP 201
     */
    @Test
    public void testCreatePidFromSimpleAndAcceptAll() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.ALL.toString());
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
    }

    /**
     * Input: (complex) json
     * Accept: json
     * Expect: (complex) json, HTTP 201
    */
    @Test
    public void testCreatePidFromComplexAndAcceptJson() throws Exception {
        PIDRecord input = ApiMockUtils.getSomePidRecordInstance();
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON.toString(), MediaType.APPLICATION_JSON.toString());
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
    }

    /**
     * Input: (complex) json
     * Accept: any
     * Expect: (complex) json, HTTP 201
     */
    @Test
    public void testCreatePidFromComplexAndAcceptAll() throws Exception {
        PIDRecord input = ApiMockUtils.getSomePidRecordInstance();
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON.toString(), MediaType.ALL.toString());
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
    }
}
