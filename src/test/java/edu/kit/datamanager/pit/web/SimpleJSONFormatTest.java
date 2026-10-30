package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
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

        SimplePidRecord simpleFormat = ApiMockUtils.resolveSimpleRecord(this.mockMvc, complexFormat.getPid());
        assertNotNull(simpleFormat);
        assertNotNull(simpleFormat.getPairs());

        assertEquals(complexFormat.getPid(), simpleFormat.getPid());
        long complexPairs = complexFormat.getEntries().values().stream().mapToLong(list -> list.size()).sum();

        System.out.println(simpleFormat.getPairs());
        assertEquals(complexPairs, simpleFormat.getPairs().size());
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
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
        String responseBody = ApiMockUtils.registerRecord(mockMvc, requestBody, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord modified = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(modified);
        assertTrue(modified.getEntries().size() > 0);
    }

    /**
     * Reproduces the e2e test `docker/tests/benchmark-create_dryrun.hurl`:
     * creates a PID record in the simple+json format with `dryrun=true`,
     * expecting HTTP 200 (validation only, no PID is registered).
     *
     * The record content equals the failing e2e scenario exactly. The body is
     * built via the JSON mapper rather than as a raw string literal, so that
     * the embedded JSON values are escaped correctly (as hurl transmits them).
     */
    @Test
    void testCreateDryrunBenchmarkRecord() throws Exception {
        // Values that contain embedded JSON are passed as raw strings and
        // escaped by the mapper on serialization, mirroring the hurl file.
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(
                Map.of("record", List.of(
                        Map.of("key", "21.T11148/076759916209e5d62bd5", "value", "21.T11148/b9b76f887845e32d29f7"),
                        Map.of("key", "21.T11148/1c699a5d1b4ad3ba4956", "value", "21.T11148/ca9fd0b2414177b79ac2"),
                        Map.of("key", "21.T11148/a753134738da82809fc1", "value", "21.T11148/a753134738da82809fc1"),
                        Map.of("key", "21.T11148/b8457812905b83046284", "value", "https://hdl.handle.net/21.T11148/b8457812905b83046284"),
                        Map.of("key", "21.T11148/1a73af9e7ae00182733b", "value", "https://orcid.org/0000-0001-6575-1022"),
                        Map.of("key", "21.T11148/aafd5fb4c7222e2d950a", "value", "2020-10-21T00:00:00+02:00"),
                        Map.of("key", "21.T11969/a00985b98dac27bd32f8", "value", "Book"),
                        Map.of("key", "21.T11148/2f314c8fe5fb6a0063a8", "value", "{\"licenseURL\": \"https://www.gnu.org/licenses/agpl-3.0.en.html\"}"),
                        Map.of("key", "21.T11148/82e2503c49209e987740", "value", "{\"md5sum\": \"2289159614f3e3b06fc436423c0dc398\"}"),
                        Map.of("key", "21.T11148/7fdada5846281ef5d461", "value", "{\"locationPreview/Sample\": \"https://example.com/my/path/to/image.svg\"}"),
                        Map.of("key", "21.T11148/6ae999552a0d2dca14d6", "value", "this-is-a-string"),
                        Map.of("key", "21.T11148/f3f0cbaa39fa9966b279", "value", "this-is-a-string"),
                        Map.of("key", "21.T11148/4fe7cde52629b61e3b82", "value", "sandboxed/some-random-pid"))));

        mockMvc.perform(
                post("/api/v1/pit/pid/")
                    .contentType(SimplePidRecord.CONTENT_TYPE)
                    .accept(SimplePidRecord.CONTENT_TYPE)
                    .characterEncoding("utf-8")
                    .param("dryrun", "true")
                    .content(requestBody))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk());
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original, modified, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertTrue(receivedRecord.getEntries().size() > 0);
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        String responseBody = ApiMockUtils.updateRecord(mockMvc, original, modified, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertTrue(receivedRecord.getEntries().size() > 0);
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertTrue(receivedRecord.getEntries().size() > 0);
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, SimplePidRecord.class);
        assertNotNull(receivedRecord);
        assertNotNull(receivedRecord.getPairs());
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.ALL_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertTrue(receivedRecord.getEntries().size() > 0);
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
        // in future, it would be more reliable to really ask the profile for the pid of this field.
        PIDRecord modified = ApiMockUtils.clone(original);
        modified.addEntry("21.T11148/b8457812905b83046284", "", "https://example.com/file2");
        SimplePidRecord simple = new SimplePidRecord(modified);
        assertNotNull(simple.getPairs());
        String requestBody = ApiMockUtils.getJsonMapper().writeValueAsString(simple);
        String responseBody = ApiMockUtils.updateRecord(mockMvc, simple.getPid(), requestBody, original.getEtag(), SimplePidRecord.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PIDRecord receivedRecord = ApiMockUtils.getJsonMapper().readValue(responseBody, PIDRecord.class);
        assertNotNull(receivedRecord);
        assertTrue(receivedRecord.getEntries().size() > 0);
    }
}
