package edu.kit.datamanager.pit.web;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpHeaders;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.domain.PidRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;

/**
 * A collection of reusable test components.
 * 
 * Usually methods wrapping often-used mockMvc calls.
 */
public class ApiMockUtils {

    static final String JSON_RECORD = "{\"entries\":{\"21.T11148/076759916209e5d62bd5\":[{\"key\":\"21.T11148/076759916209e5d62bd5\",\"name\":\"kernelInformationProfile\",\"value\":\"21.T11148/301c6f04763a16f0f72a\"}],\"21.T11148/397d831aa3a9d18eb52c\":[{\"key\":\"21.T11148/397d831aa3a9d18eb52c\",\"name\":\"dateModified\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/8074aed799118ac263ad\":[{\"key\":\"21.T11148/8074aed799118ac263ad\",\"name\":\"digitalObjectPolicy\",\"value\":\"21.T11148/37d0f4689c6ea3301787\"}],\"21.T11148/92e200311a56800b3e47\":[{\"key\":\"21.T11148/92e200311a56800b3e47\",\"name\":\"etag\",\"value\":\"{ \\\"sha256sum\\\": \\\"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\\\" }\"}],\"21.T11148/aafd5fb4c7222e2d950a\":[{\"key\":\"21.T11148/aafd5fb4c7222e2d950a\",\"name\":\"dateCreated\",\"value\":\"2021-12-21T17:36:09.541+00:00\"}],\"21.T11148/b8457812905b83046284\":[{\"key\":\"21.T11148/b8457812905b83046284\",\"name\":\"digitalObjectLocation\",\"value\":\"https://test.repo/file001\"}],\"21.T11148/c692273deb2772da307f\":[{\"key\":\"21.T11148/c692273deb2772da307f\",\"name\":\"version\",\"value\":\"1.0.0\"}],\"21.T11148/c83481d4bf467110e7c9\":[{\"key\":\"21.T11148/c83481d4bf467110e7c9\",\"name\":\"digitalObjectType\",\"value\":\"21.T11148/ManuscriptPage\"}]},\"pid\":\"unregistered-18622\"}";

    /**
     * Retrieves an object mapper which is the same as the bean in the
     * application context, from a functional point of view.
     */
    public static ObjectMapper getJsonMapper() {
        return Application.jsonObjectMapper();
    }

    /**
     * A fast way to get a valid PIDRecord instance.
     * 
     * @return a valid PIDRecord instance.
     * @throws JacksonException on error.
     */
    public static PidRecord getSomePidRecordInstance() throws JacksonException {
        return getJsonMapper().readValue(JSON_RECORD, PidRecord.class);
    }

    /**
     * Wrapper to query known PIDs via API given time intervals for the creation
     * timestamp and modification timestamp.
     * 
     * @param createdAfter   lower end for the creation timestamp interval
     * @param createdBefore  upper end for the creation timestamp interval
     * @param modifiedAfter  lower end for the modification timestamp interval
     * @param modifiedBefore upper end for the modification timestamp interval
     * @param pageable       an optional parameter to indicate the page which should
     *                       be returned
     * @return the result of the query
     * @throws Exception on failed assumptions
     */
    public static List<KnownPid> queryKnownPIDs(
        MockMvc mockMvc,
        Instant createdAfter,
        Instant createdBefore,
        Instant modifiedAfter,
        Instant modifiedBefore,
        Optional<Pageable> pageable
    ) throws Exception {
        MockHttpServletRequestBuilder request =  get("/api/v1/pit/known-pid");
        if (pageable.isPresent()) {
            request.param("page", String.valueOf(pageable.get().getPageNumber()));
            request.param("size", String.valueOf(pageable.get().getPageSize()));
        }
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
        MvcResult result = mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        List<KnownPid> pidinfos = Arrays.asList(getJsonMapper().readerForArrayOf(KnownPid.class).readValue(body));
        return pidinfos;
    }

    public static JsonNode queryKnownPIDsInTabulatorFormat(
        MockMvc mockMvc,
        Instant createdAfter,
        Instant createdBefore,
        Instant modifiedAfter,
        Instant modifiedBefore,
        Optional<Pageable> pageable
    ) throws Exception {
        MockHttpServletRequestBuilder request =  get("/api/v1/pit/known-pid");
        if (pageable.isPresent()) {
            request.param("page", String.valueOf(pageable.get().getPageNumber()));
            request.param("size", String.valueOf(pageable.get().getPageSize()));
        }
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
        request.accept("application/tabulator+json");

        MvcResult result = mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return getJsonMapper().readTree(body);
    }

    /**
     * Updates a PID record.
     * 
     * @param newRecord the record, containing the information as it should be after
     *               the update.
     * @return the record as it is after the update.
     * @throws Exception if any assumption breaks.
     */
    public static PidRecord updateRecord(MockMvc mockMvc, PidRecord oldRecord, PidRecord newRecord) throws Exception {
        String body = updateRecord(
            mockMvc,
            oldRecord,
            newRecord,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.ALL_VALUE
        );
        return deserializeRecord(body);
    }

    public static MvcResult updateRecordAndReturnMvcResult(
        MockMvc mockMvc,
        PidRecord oldRecord,
        PidRecord newRecord) throws Exception
    {
        String pid = oldRecord.getPid();
        String etag = oldRecord.getEtag();
        String body = serialize(newRecord);
        assertEquals(oldRecord.getPid(), newRecord.getPid());
        return updateRecordAndReturnMvcResult(mockMvc, pid, body, etag, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
    }

    /**
     * Updates a PID record. 
     *
     * @param mockMvc instance that mocks the REST API.
     * @param oldRecord
     * @param newRecord
     * @param bodyContentType type of the body.
     * @param acceptContentType type to expect.
     * @return the response body.
     * @throws Exception if response is not OK (HTTP 200).
     */
    public static String updateRecord(
            MockMvc mockMvc,
            PidRecord oldRecord,
            PidRecord newRecord,
            String bodyContentType,
            String acceptContentType) throws Exception
    {
        String pid = oldRecord.getPid();
        String etag = oldRecord.getEtag();
        String body = serialize(newRecord);
        assertEquals(oldRecord.getPid(), newRecord.getPid());
        return updateRecordAndReturnMvcResult(mockMvc, pid, body, etag, bodyContentType, acceptContentType)
            .getResponse().getContentAsString();
    }

    public static String updateRecord(
        MockMvc mockMvc,
        String pid,
        String body,
        String etag,
        String bodyContentType,
        String acceptContentType) throws Exception
    {
        return updateRecordAndReturnMvcResult(mockMvc, pid, body, etag, bodyContentType, acceptContentType)
            .getResponse().getContentAsString();
    }

    private static MvcResult updateRecordAndReturnMvcResult(
            MockMvc mockMvc,
            String pid,
            String body,
            String etag,
            String bodyContentType,
            String acceptContentType) throws Exception
    {
        assertFalse(pid.isEmpty());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/api/v1/pit/pid/".concat(pid))
            .characterEncoding("utf-8")
            .content(body)
            .header(HttpHeaders.IF_MATCH, quoted(etag));
        boolean hasAcceptContentType = acceptContentType != null && !acceptContentType.isEmpty();
        boolean hasBodyContentType = bodyContentType != null && !bodyContentType.isEmpty();
        if (hasAcceptContentType) {
            request = request.accept(acceptContentType);
        }
        if (hasBodyContentType) {
            request = request.contentType(bodyContentType);
        }
        return mockMvc
                .perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    /**
     * Resolves a record using the REST API and MockMvc.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @param pid the PID to resolve
     * @return the resolved record of the given PID.
     * @throws Exception if any assumption breaks.
     */
    public static PidRecord resolveRecord(MockMvc mockMvc, String pid) throws Exception {
        String resolvedBody = ApiMockUtils.resolveRecord(mockMvc, pid, null);
        PidRecord resolvedRecord = getJsonMapper().readValue(resolvedBody, PidRecord.class);
        return resolvedRecord;
    }

    /**
     * Resolves a record using the REST API and MockMvc.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @param pid the PID to resolve
     * @return the resolved record of the given PID.
     * @throws Exception if any assumption breaks.
     */
    public static SimplePidRecord resolveSimpleRecord(MockMvc mockMvc, String pid) throws Exception {
        String resolvedBody = ApiMockUtils.resolveRecord(mockMvc, pid, SimplePidRecord.CONTENT_TYPE);
        SimplePidRecord resolvedRecord = getJsonMapper().readValue(resolvedBody, SimplePidRecord.class);
        return resolvedRecord;
    }

    /**
     * Resolves a record using the REST API and MockMvc.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @param createdPid the PID to resolve.
     * @param contentType the content type for the request.
     * @return the resolved record of the given PID.
     * @throws Exception if any assumption breaks.
     */
    private static String resolveRecord(MockMvc mockMvc, String pid, String contentType) throws Exception {
        return resolveRecordAndReturnMvcResult(mockMvc, pid, contentType).getResponse().getContentAsString();
    }

    public static MvcResult resolveRecordAndReturnMvcResult(MockMvc mockMvc, String pid) throws Exception {
        return ApiMockUtils.resolveRecordAndReturnMvcResult(mockMvc, pid, null);
    }

    private static MvcResult resolveRecordAndReturnMvcResult(MockMvc mockMvc, String pid, String contentType) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/pit/pid/".concat(pid));
        if (contentType != null && !contentType.isEmpty()) {
            request = request.accept(contentType);
        }
        return mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    /**
     * Creates a record and does make some generic tests.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @return The created PID record.
     * @throws Exception if any assumption breaks.
     */
    public static PidRecord registerSomeRecord(MockMvc mockMvc) throws Exception {
        MvcResult result = ApiMockUtils.registerRecordAndGetMvcResult(mockMvc, ApiMockUtils.JSON_RECORD, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PidRecord createdRecord = ApiMockUtils.deserializeRecord(result);
        String createdPid = createdRecord.getPid();
        assertFalse(createdPid.isEmpty());
        return createdRecord;
    }

    public static MvcResult registerSomeRecordAndReturnMvcResult(MockMvc mockMvc) throws Exception {
    return ApiMockUtils.registerRecordAndGetMvcResult(
        mockMvc,
        ApiMockUtils.JSON_RECORD, // some default record
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.ALL_VALUE);
    }

    /**
     * Generic method to do a "create" request. Expects HTTP 201.
     * 
     * @param mockMvc instance that mocks the REST API
     * @param body the PID record to create
     * @param bodyContentType type of the body
     * @param acceptContentType type to expect
     * @return the body of the response as string.
     * @throws Exception on any error.
     */
    public static String registerRecord(MockMvc mockMvc, String body, String bodyContentType, String acceptContentType) throws Exception {
        return ApiMockUtils.registerRecordAndGetMvcResult(mockMvc, body, bodyContentType, acceptContentType)
            .getResponse()
            .getContentAsString();
    }

    /**
     * Generic method to do a "create" request.
     * 
     * @param mockMvc instance that mocks the REST API
     * @param body the PID record to create
     * @param bodyContentType type of the body
     * @param acceptContentType type to expect
     * @param expectHttpCode custom result matcher for expecting other HTTP response codes.
     * @return the body of the response as string.
     * @throws Exception on any error.
     */
    public static String registerRecord(
        MockMvc mockMvc,
        String body,
        String bodyContentType,
        String acceptContentType,
        ResultMatcher expectHttpCode) throws Exception
    {
        MvcResult created = registerRecordAndGetMvcResult(mockMvc, body, bodyContentType, acceptContentType, expectHttpCode);
        return created.getResponse().getContentAsString();
    }

    public static MvcResult registerRecordAndGetMvcResult(
        MockMvc mockMvc,
        String body,
        String bodyContentType,
        String acceptContentType) throws Exception
    {
        return registerRecordAndGetMvcResult(mockMvc, body, bodyContentType, acceptContentType, MockMvcResultMatchers.status().isCreated());
    }

    public static ResultActions registerRecordAndGetResultActions(
        MockMvc mockMvc,
        String body,
        String bodyContentType,
        String acceptContentType
    ) throws Exception {
            MockHttpServletRequestBuilder request = post("/api/v1/pit/pid/")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(body);
        boolean hasAcceptContentType = acceptContentType != null && !acceptContentType.isEmpty();
        boolean hasBodyContentType = bodyContentType != null && !bodyContentType.isEmpty();
        if (hasAcceptContentType) {
            request = request.accept(acceptContentType);
        } else {
            request = request.accept(MediaType.ALL);
        }
        if (hasBodyContentType) {
            request = request.contentType(bodyContentType);
        } else {
            request = request.contentType(MediaType.APPLICATION_JSON);
        }
        return mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print());
        }

    public static MvcResult registerRecordAndGetMvcResult(
        MockMvc mockMvc,
        String body,
        String bodyContentType,
        String acceptContentType,
        ResultMatcher expectHttpCode) throws Exception
    {
        return registerRecordAndGetResultActions(
            mockMvc,
            body,
            bodyContentType,
            acceptContentType
        )
            .andExpect(expectHttpCode)
            .andReturn();
    }

    public static PidRecord clone(PidRecord original) throws JsonMappingException, JsonProcessingException {
        String body = serialize(original);
        return deserializeRecord(body);
    }

    public static String serialize(PidRecord pidRecord) throws JsonProcessingException {
        return getJsonMapper().writeValueAsString(pidRecord);
    }

    public static PidRecord deserializeRecord(MvcResult result) throws Exception {
        return deserializeRecord(result.getResponse());
    }

    public static PidRecord deserializeRecord(MockHttpServletResponse response) throws Exception {
        String body = response.getContentAsString();
        return deserializeRecord(body);
    }

    public static PidRecord deserializeRecord(String body) throws JsonMappingException, JsonProcessingException {
        return getJsonMapper().readValue(body, PidRecord.class);
    }

    public static String quoted(String etag) {
        return String.format("\"%s\"", etag);
    }
}
