package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class SimpleJSONFormatTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    /**
     * Test: Resolve PID
     */
    @Test
    void testResolvePid() throws Exception {
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
     * Test: Create PID
     * 
     * Input: simple json
     * Accept: simple json
     * Expect: simple json, HTTP 201
     */
    @Test
    void testCreatePidFromSimpleAndAcceptSimple() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
        assertNotNull(sim.getPairs());
    }

    /**
     * Test: Create PID
     * 
     * Input: simple json
     * Accept: any
     * Expect: (complex) json, HTTP 201
     */
    @Test
    void testCreatePidFromSimpleAndAcceptAll() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Create PID
     * 
     * Input: (complex) json
     * Accept: json
     * Expect: (complex) json, HTTP 201
    */
    @Test
    void testCreatePidFromComplexAndAcceptJson() throws Exception {
        PIDRecord input = ApiMockUtils.getSomePidRecordInstance();
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Create PID
     * 
     * Input: (complex) json
     * Accept: any
     * Expect: (complex) json, HTTP 201
     */
    @Test
    void testCreatePidFromComplexAndAcceptAll() throws Exception {
        PIDRecord input = ApiMockUtils.getSomePidRecordInstance();
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String responseBody = ApiMockUtils.createRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Update PID
     * 
     * Input: (complex) json
     * Accept: any
     * Expect: (complex) json, HTTP 200
     */
    @Test
    void testUpdatePidFromComplexAndAcceptAll() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(original);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original.getPid(), requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Update PID
     * 
     * Input: (complex) json
     * Accept: json
     * Expect: (complex) json, HTTP 200
     */
    @Test
    void testUpdatePidFromComplexAndAcceptComplex() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(original);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original.getPid(), requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Update PID
     * 
     * Input: simple json
     * Accept: json
     * Expect: (complex) json, HTTP 200
     */
    @Test
    void testUpdatePidFromSimpleAndAcceptJson() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(original);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Update PID
     * 
     * Input: simple json
     * Accept: simple json
     * Expect: simple json, HTTP 200
     */
    @Test
    void testUpdatePidFromSimpleAndAcceptSimple() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(original);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(modified);
        assertNotNull(modified.getPairs());
    }

    /**
     * Test: Update PID
     * 
     * Input: simple json
     * Accept: any
     * Expect: (complex) json, HTTP 200
     */
    @Test
    void testUpdatePidFromSimpleAndAcceptAll() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(original);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Test: Update PID
     * 
     * Input: simple json
     * Accept: (complex) json
     * Expect: complex json, HTTP 200
     */
    @Test
    void testUpdatePidFromSimpleAndAcceptComplex() throws Exception {
        PIDRecord original = ApiMockUtils.createSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(original);
        assertNotNull(simple.getPairs());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }
}
