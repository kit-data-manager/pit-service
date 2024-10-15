package edu.kit.datamanager.pit.web;

import org.apache.http.entity.ContentType;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    /**
     * Test: Resolve PID
     */
    @Test
    void testResolvePid() throws Exception {
        PIDRecord complexFormat = ApiMockUtils.registerSomeRecord(this.mockMvc);
        assertNotNull(complexFormat);

        SimplePidRecord simpleFormat = ApiMockUtils.resolveSimpleRecord(this.mockMvc, complexFormat.pid());
        assertNotNull(simpleFormat);
        assertNotNull(simpleFormat.pairs());

        assertEquals(complexFormat.pid(), simpleFormat.pid());
        long complexPairs = complexFormat.entries().values().stream().mapToLong(List::size).sum();

        System.out.println(simpleFormat.pairs());
        assertEquals(complexPairs, simpleFormat.pairs().size());
    }

    /**
     * Test: Create PID with unsupported accept type
     * 
     * Input: simple json
     * Accept: SVG/XML
     * Expect: error, HTTP 406
     */
    @Test
    void testCreateWithUnsupportedAcceptType() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String detailMessage = "Acceptable representations: [" + ContentType.APPLICATION_JSON.getMimeType() + ", " + SimplePidRecord.CONTENT_TYPE + "].";
        ApiMockUtils.registerRecordAndGetResultActions(
            mockMvc,
            requestBody,
            SimplePidRecord.CONTENT_TYPE,
            ContentType.APPLICATION_SVG_XML.getMimeType()
        )
            .andExpect(MockMvcResultMatchers.status().isNotAcceptable())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.is(detailMessage)));
    }

    /**
     * Test: Create PID with unsupported content type
     * 
     * Input: SVG/XML
     * Accept: simple json
     * Expect: error, HTTP 415
     */
    @Test
    void testCreateWithUnsupportedContentType() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String detailMessage = "Content-Type '" + ContentType.APPLICATION_SVG_XML.getMimeType() + "' is not supported.";
        ApiMockUtils.registerRecordAndGetResultActions(
            mockMvc,
            requestBody,
            ContentType.APPLICATION_SVG_XML.getMimeType(),
            SimplePidRecord.CONTENT_TYPE
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.is(detailMessage)));
    }

    /**
     * Test: Create PID with unsupported content type
     * 
     * Input: SVG/XML
     * Accept: SVG/XML
     * Expect: error, HTTP 415
     */
    @Test
    void testCreateWithUnsupportedMediaTypes() throws Exception {
        SimplePidRecord input = new SimplePidRecord(ApiMockUtils.getSomePidRecordInstance());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(input);
        String detailMessage = "Content-Type '" + ContentType.APPLICATION_SVG_XML.getMimeType() + "' is not supported.";
        ApiMockUtils.registerRecordAndGetResultActions(
            mockMvc,
            requestBody,
            ContentType.APPLICATION_SVG_XML.getMimeType(),
            SimplePidRecord.CONTENT_TYPE
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.is(detailMessage)));
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord sim = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(sim);
        assertNotNull(sim.pairs());
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertFalse(modified.entries().isEmpty());
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertFalse(modified.entries().isEmpty());
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertFalse(modified.entries().isEmpty());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        String etag = original.getEtag();
        PIDRecord modified = original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        assertEquals(etag, original.getEtag());
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original, modified, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertFalse(receivedRecord.entries().isEmpty());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original, modified, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertFalse(receivedRecord.entries().isEmpty());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = original.addEntry(
                "21.T11148/b8457812905b83046284",
                "",
                "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.pid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertFalse(receivedRecord.entries().isEmpty());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.pid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(receivedRecord);
        assertNotNull(receivedRecord.pairs());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.pid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertFalse(receivedRecord.entries().isEmpty());
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
        PIDRecord original = ApiMockUtils.registerSomeRecord(this.mockMvc);
        // add digitalObjectLocation, which is a property supported by most profiles.
        // In the future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = original.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        assertNotNull(simple.pairs());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.pid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertFalse(receivedRecord.entries().isEmpty());
    }
}
