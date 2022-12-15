package edu.kit.datamanager.pit.web;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.domain.PIDRecord;
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
    public static PIDRecord getSomePidRecordInstance() throws JacksonException {
        return getJsonMapper().readValue(JSON_RECORD, PIDRecord.class);
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
        MockHttpServletRequestBuilder request =  get("/api/v1/pit/known-pid/");
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
        MockHttpServletRequestBuilder request =  get("/api/v1/pit/known-pid/");
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
     * @param record the record, containing the information as it should be after
     *               the update.
     * @return the record as it is after the update.
     * @throws Exception if any assumption breaks.
     */
    public static PIDRecord updateRecord(MockMvc mockMvc, PIDRecord record) throws Exception {
        String body = updateRecord(
            mockMvc,
            record.getPid(),
            getJsonMapper().writeValueAsString(record),
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.ALL_VALUE
        );
        PIDRecord updatedRecord = getJsonMapper().readValue(body, PIDRecord.class);
        return updatedRecord;
    }

    /**
     * Updates a PID record.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @param pid the PID to update.
     * @param body the updated pid record to send.
     * @param bodyContentType type of the body.
     * @param acceptContentType type to expect.
     * @return the response body.
     * @throws Exception if response is not OK (HTTP 200).
     */
    public static String updateRecord(MockMvc mockMvc, String pid, String body, String bodyContentType, String acceptContentType) throws Exception {
        assertFalse(pid.isEmpty());
        MockHttpServletRequestBuilder request = put("/api/v1/pit/pid/" + pid)
            .characterEncoding("utf-8")
            .content(body);
        boolean hasAcceptContentType = acceptContentType != null && !acceptContentType.isEmpty();
        boolean hasBodyContentType = bodyContentType != null && !bodyContentType.isEmpty();
        if (hasAcceptContentType) {
            request = request.accept(acceptContentType);
        }
        if (hasBodyContentType) {
            request = request.contentType(bodyContentType);
        }
        MvcResult updated = mockMvc
                .perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        return updated.getResponse().getContentAsString();
    }

    /**
     * Resolves a record using the REST API and MockMvc.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @param pid the PID to resolve
     * @return the resolved record of the given PID.
     * @throws Exception if any assumption breaks.
     */
    public static PIDRecord resolveRecord(MockMvc mockMvc, String pid) throws Exception {
        String resolvedBody = ApiMockUtils.resolveRecord(mockMvc, pid, null);
        PIDRecord resolvedRecord = getJsonMapper().readValue(resolvedBody, PIDRecord.class);
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
        MockHttpServletRequestBuilder request = get("/api/v1/pit/pid/".concat(pid));
        if (contentType != null && !contentType.isEmpty()) {
            request = request.accept(contentType);
        }
        MvcResult resolved = mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        return resolved.getResponse().getContentAsString();
    }

    /**
     * Creates a record and does make some generic tests.
     * 
     * @param mockMvc instance that mocks the REST API.
     * @return The created PID record.
     * @throws Exception if any assumption breaks.
     */
    public static PIDRecord createSomeRecord(MockMvc mockMvc) throws Exception {
        String createdBody = ApiMockUtils.createRecord(mockMvc, ApiMockUtils.JSON_RECORD, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE);
        PIDRecord createdRecord = getJsonMapper().readValue(createdBody, PIDRecord.class);
        String createdPid = createdRecord.getPid();
        assertFalse(createdPid.isEmpty());
        return createdRecord;
    }

    /**
     * Generic method to do a "create" request.
     * 
     * @param mockMvc instance that mocks the REST API
     * @param body the PID record to create
     * @param bodyContentType type of the body
     * @param acceptContentType type to expect
     * @return the body of the response as string.
     * @throws Exception on any error.
     */
    public static String createRecord(MockMvc mockMvc, String body, String bodyContentType, String acceptContentType) throws Exception {
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
        MvcResult created = mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();
        return created.getResponse().getContentAsString();
    }
}
