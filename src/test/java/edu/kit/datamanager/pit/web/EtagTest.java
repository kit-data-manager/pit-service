package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.kit.datamanager.pit.domain.PIDRecord;

@AutoConfigureMockMvc
// JUnit5 + Spring
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class EtagTest {

    private static final String RESOLVE_URL = "/api/v1/pit/pid/";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private PIDRecord existingRecord;

    /*
     * Pre-test to make sure there is at least one PID available.
     * 
     * Input: no etag
     * Action: create
     * Expect: HTTP 201, etag in header, etag matches record
     */
    @BeforeEach
    @Test
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();

        MockHttpServletResponse response = ApiMockUtils.registerSomeRecordAndReturnMvcResult(this.mockMvc).getResponse();
        String etagHeader = response.getHeader(HttpHeaders.ETAG);
        this.existingRecord = ApiMockUtils.deserializeRecord(response);
        String etagRecord = ApiMockUtils.deserializeRecord(response).getEtag();
        assertNotNull(etagHeader);
        assertFalse(etagHeader.isBlank());
        assertEquals(etagRecord, etagHeader.replaceAll("\"", ""));
    }

    /**
     * Input: no Etag (pid only)
     * Action: resolve
     * Expect: Etag header in response matches Etag of PID record, and HTTP 200 (OK)
     */
    @Test
    void givenNoEtag_onResolve_returnOk() throws Exception {
        // retrieving will result in http 200 (OK)
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(RESOLVE_URL.concat(this.existingRecord.pid()))
            .accept(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        
        String etag = result
            .getResponse()
            .getHeader(HttpHeaders.ETAG).replaceAll("\"", "");
        assertEquals(this.existingRecord.getEtag(), etag);
    }

    /*
     * Input: matching etag (If-None-Match)
     * Action: resolve
     * Expect: Not Modified (304)
     */
    @Test
    void givenIfNoneMatchEtag_onResolve_returnNotModified() throws Exception {
        String etag = quoted(this.existingRecord.getEtag());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RESOLVE_URL.concat(this.existingRecord.pid()))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, etag);
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotModified())
            .andReturn();
    }

    /*
     * Input: invalid etag (If-None-Match)
     * Action: resolve
     * Expect: OK (200)
     */
    @Test
    void givenInvalidIfNoneMatchEtag_onResolve_returnOk() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RESOLVE_URL.concat(this.existingRecord.pid()))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, "somethingElse");
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    /*
     * Input: matching etag (If-Match)
     * Action: resolve
     * Expect: Not Modified (304)
     */
    @Test
    void givenIfMatchEtag_onResolve_returnNotModified() throws Exception {
        String etag = quoted(this.existingRecord.getEtag());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RESOLVE_URL.concat(this.existingRecord.pid()))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, etag);
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotModified())
            .andReturn();
    }

    /*
     * Input: invalid etag (If-Match)
     * Action: resolve
     * Expect: Ok (200)
     */
    @Test
    void givenIfMatchEtag_onResolve_returnOk() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RESOLVE_URL.concat(this.existingRecord.pid()))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, "somethingElse");
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    /*
     * Input: no etag
     * Action: update
     * Expect: Precondition Required (428)
     */
    @Test
    void givenNoEtag_onUpdate_returnPreconditionRequired() throws Exception {
        // let's update the existing record without etag
        PIDRecord modified = this.existingRecord.addEntry("21.T11148/d0773859091aeb451528", "", "fake/pid");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(modified.pid()))
            .characterEncoding("utf-8")
            .content(ApiMockUtils.serialize(modified))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isPreconditionRequired())
            .andReturn();
    }

    /*
     * Input: valid etag (If-Match)
     * Action: update
     * Expect: 200
     */
    @Test
    void givenIfMatchEtag_onUpdate_returnOk() throws Exception {
        String oldEtag = quoted(this.existingRecord.getEtag());
        // let's update the existing record without etag
        PIDRecord modified = this.existingRecord.addEntry("21.T11148/397d831aa3a9d18eb52c", "", "2016-05-03T15:15:07.473Z");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(modified.pid()))
            .characterEncoding("utf-8")
            .content(ApiMockUtils.serialize(modified))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_MATCH, oldEtag);
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    /*
     * Input: valid etag (If-None-Match)
     * Action: update
     * Expect: Precondition Required (428)
     * 
     * We do not allow If-None-Match for updates, we need If-Match.
     */
    @Test
    void givenIfNoneMatchEtag_onUpdate_returnPreconditionRequired() throws Exception {
        // let's update the existing record without etag
        PIDRecord modified = this.existingRecord.addEntry("21.T11148/397d831aa3a9d18eb52c", "", "2016-05-03T15:15:07.473Z");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(modified.pid()))
            .characterEncoding("utf-8")
            .content(ApiMockUtils.serialize(modified))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, quoted("somethingElse"));
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isPreconditionRequired())
            .andReturn();
    }

    /*
     * Input: invalid etag (If-Match)
     * Action: update
     * Expect: Precondition Failed (412)
     */
    @Test
    void givenInvalidIfMatchEtag_onUpdate_returnPreconditionFailed() throws Exception {
        // let's update the existing record without etag
        PIDRecord modified = this.existingRecord.addEntry("21.T11148/d0773859091aeb451528", "", "fake/pid");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(modified.pid()))
            .characterEncoding("utf-8")
            .content(ApiMockUtils.serialize(modified))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_MATCH, quoted("somethingElse"));
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isPreconditionFailed())
            .andReturn();
    }

    /*
     * Input: invalid etag (If-None-Match)
     * Action: update
     * Expect: Precondition Required (428)
     * 
     * We do not allow If-None-Match for updates, we need If-Match.
     */
    @Test
    void givenInvalidIfNoneEtag_onUpdate_returnPreconditionFailed() throws Exception {
        // let's update the existing record without etag
        PIDRecord modified = this.existingRecord.addEntry("21.T11148/d0773859091aeb451528", "", "fake/pid");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(modified.pid()))
            .characterEncoding("utf-8")
            .content(ApiMockUtils.serialize(modified))
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.IF_NONE_MATCH, quoted("somethingElse"));
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isPreconditionRequired())
            .andReturn();
    }

    private String quoted(String etag) {
        return String.format("\"%s\"", etag);
    }
}
