/*
 * Copyright (c) 2024-2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.pit.web;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * @author jejkal
 */
public interface ITypingRestResource {

    /**
     * Create multiple, possibly related PID records using the record information.
     * This endpoint is a convenience method to create multiple PID records at once.
     * For connecting records, the PID fields must be specified and the value may be used in the value fields of other PIDRecordEntries.
     * The provided PIDs will be overwritten as defined by the PID generator strategy.
     * <p>
     * Note: This endpoint does not support custom PIDs, as the PID field is used for "imaginary" PIDs to connect records.
     * These "imaginary" PIDs will be overwritten with actual, resolvable PIDs as defined by the PID generator strategy.
     * If you want to create a record with custom PIDs, use the endpoint `POST /pid`.
     *
     * @param rec    A list of PID records.
     * @param dryrun If true, only validation will be done and no PIDs will be created. No data will be changed and no services will be notified.
     * @return either 201 and a list of record representations, or an error (see ApiResponse annotations and tests).
     * @throws IOException                                              if an error occurs.
     * @throws edu.kit.datamanager.pit.common.RecordValidationException if any of the records is invalid or a PID was used for multiple records in the same request.
     */
    @PostMapping(
            path = "pids",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            summary = "Create a multiple, possibly related PID records",
            description = "Create multiple, possibly related PID records using the record information from the request body. To connect records, the PID fields must be specified. This 'imaginary' PID value may then be used in the value fields of other PID Record entries. During creation, these `imaginary` PIDs whose sole purpose is to connect records will be overwritten with actual, resolvable PIDs as defined by the PID generator strategy. Note: This procedure does not support custom PIDs, as the PID field is used for linking records. If you want to create a record with custom PIDs, use the endpoint `POST /pid`."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "The body containing a list of all PID record values as they should be in the new PID records. To connect records, the PID fields must be specified. This 'imaginary' PID value may then be used in the value fields of other PID Record entries. During creation, these `imaginary` PIDs whose sole purpose is to connect records will be overwritten with actual, resolvable PIDs as defined by the PID generator strategy.",
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PIDRecord.class)))
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Successfully created all records and resolved references (if they exist). The response contains the created records.",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PIDRecord.class)))
                    }),
            @ApiResponse(responseCode = "400", description = "Validation failed. See body for details. Contains also the validated records.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "406", description = "Provided input is invalid with regard to the supported accept header (Not acceptable)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "415", description = "Provided input is invalid with regard to the supported content types. (Unsupported Mediatype)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "If providing own PIDs is enabled 409 indicates, that the PID already exists.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "503", description = "Communication to required external service failed.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<List<PIDRecord>> createPIDs(
            @RequestBody final List<PIDRecord> rec,

            @Parameter(description = "If true, only validation will be done and no PIDs will be created. No data will be changed and no services will be notified.", required = false)
            @RequestParam(name = "dryrun", required = false, defaultValue = "false")
            boolean dryrun,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Create a new PID using the record information provided in the request body.
     * The record is expected to contain the identifier of the matching profile.
     * Before creating the record, the record information will be validated against
     * the profile.
     * <p>
     * Important note: Validation caches recently used type information locally.
     * Therefore, changes in a registry may take a few minutes to be reflected
     * within the Typed PID Maker. This speeds up validation drastically in most
     * situations. But it also means that, if the cache is empty, validation may
     * take 30+ seconds. We are aware of the issue and considering improvements. But
     * be aware that in general, validation may take up some time.
     *
     * @param rec The PID record.
     * @return either 201 and a record representation, or an error (see ApiResponse
     * annotations and tests).
     * @throws IOException
     */
    @PostMapping(
            path = "pid/",
            consumes = {MediaType.APPLICATION_JSON_VALUE, SimplePidRecord.CONTENT_TYPE},
            produces = {MediaType.APPLICATION_JSON_VALUE, SimplePidRecord.CONTENT_TYPE}
    )
    @Operation(
            summary = "Create a new PID record",
            description = "Create a new PID record using the record information from the request body." +
                    " The record may contain the identifier(s) of the matching profile(s)." +
                    " Before creating the record, the record information will be validated against" +
                    " the profile." +
                    " Validation takes some time, depending on the context. It depends a lot on the size" +
                    " of your record and the already cached information. This information is gathered" +
                    " from external services. If there are connection issues or hickups at these sites," +
                    " validation may even take up to a few seconds. Usually you can expect the request" +
                    " to be between 100ms up to 1000ms on a fast machine with reliable connections."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "The body containing all PID record values as they should be in the new PIDs record.",
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PIDRecord.class)),
                    @Content(mediaType = SimplePidRecord.CONTENT_TYPE, schema = @Schema(implementation = SimplePidRecord.class))
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PIDRecord.class)),
                            @Content(mediaType = SimplePidRecord.CONTENT_TYPE, schema = @Schema(implementation = SimplePidRecord.class))
                    }),
            @ApiResponse(responseCode = "400", description = "Validation failed. See body for details. Contains also the validated record.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "406", description = "Provided input is invalid with regard to the supported accept header (Not acceptable)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "415", description = "Provided input is invalid with regard to the supported content types. (Unsupported Mediatype)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "If providing an own PID is enabled 409 indicates, that the PID already exists.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "503", description = "Communication to required external service failed.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<PIDRecord> createPID(
            @RequestBody final PIDRecord rec,

            @Parameter(
                    description = "If true, only validation will be done" +
                            " and no PID will be created. No data will be changed" +
                            " and no services will be notified.",
                    required = false
            )
            @RequestParam(name = "dryrun", required = false, defaultValue = "false")
            boolean dryrun,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Update the given PIDs record using the information provided in the request
     * body. The record is expected to contain the identifier of the matching
     * profile. Conditions for a valid record are the same as for creation.
     * <p>
     * Important note: Validation may take up to 30+ seconds. For details, see the
     * documentation of "POST /pid/".
     *
     * @param rec the PID record.
     * @return the record (on success).
     * @throws IOException
     */
    @PutMapping(
            path = "pid/**",
            consumes = {MediaType.APPLICATION_JSON_VALUE, SimplePidRecord.CONTENT_TYPE},
            produces = {MediaType.APPLICATION_JSON_VALUE, SimplePidRecord.CONTENT_TYPE}
    )
    @Operation(
            summary = "Update an existing PID record",
            description = "Update an existing PID record using the record information from the request body." +
                    " The record may contain the identifier(s) of the matching profiles. Conditions for a" +
                    " valid record are the same as for creation." +
                    " Important note: Validation may take some time. For details, see the documentation of" +
                    " \"POST /pid/\"."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "The body containing all PID record values as they should be after the update.",
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PIDRecord.class)),
                    @Content(mediaType = SimplePidRecord.CONTENT_TYPE, schema = @Schema(implementation = SimplePidRecord.class))
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success.",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PIDRecord.class)),
                            @Content(mediaType = SimplePidRecord.CONTENT_TYPE, schema = @Schema(implementation = SimplePidRecord.class))
                    }),
            @ApiResponse(responseCode = "400", description = "Validation failed. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "406", description = "Provided input is invalid with regard to the supported accept header (Not acceptable)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "415", description = "Provided input is invalid with regard to the supported content types. (Unsupported Mediatype)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "412", description = "ETag comparison failed (Precondition failed)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "428", description = "No ETag given in If-Match header (Precondition required)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "503", description = "Communication to required external service failed.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<PIDRecord> updatePID(
            @RequestBody final PIDRecord rec,

            @Parameter(
                    description = "If true, no PID will be updated. Only" +
                            " validation checks are performed, and the expected" +
                            " response, including the new eTag, will be returned." +
                            " No data will be changed and no services will be" +
                            " notified.",
                    required = false
            )
            @RequestParam(name = "dryrun", required = false, defaultValue = "false")
            boolean dryrun,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    @GetMapping(
            path = "pid/**",
            produces = {MediaType.APPLICATION_JSON_VALUE, SimplePidRecord.CONTENT_TYPE}
    )
    @Operation(
            summary = "Get the record of the given PID.",
            description = "Get the record to the given PID, if it exists. May also be used to test" +
                    " if a PID exists. No validation is performed by default."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Found",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PIDRecord.class)),
                            @Content(mediaType = SimplePidRecord.CONTENT_TYPE, schema = @Schema(implementation = SimplePidRecord.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Validation failed. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "503", description = "Communication to required external service failed.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<PIDRecord> getRecord(
            @Parameter(
                    description = "If true, validation will be run on the" +
                            " resolved PID. On failure, an error will be" +
                            " returned. On success, the PID will be resolved.",
                    required = false
            )
            @RequestParam(name = "validation", required = false, defaultValue = "false")
            boolean validation,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Requests a PID from the local store. If this PID is known, it will be
     * returned together with the timestamps of creation and modification executed
     * on this PID by this service.
     * <p>
     * This store is not a cache! Instead, the service remembers every PID which it
     * created (and resolved, depending on the configuration parameter
     * `pit.storage.strategy` of the service) on request.
     *
     * @return the known PID and its timestamps.
     * @throws IOException
     */
    @Operation(
            summary = "Returns a PID and its timestamps from the local store, if available.",
            description = "Returns a PID from the local store. This store is not a cache! Instead, the"
                    + " service remembers every PID which it created (and resolved, depending on the"
                    + " configuration parameter `pit.storage.strategy` of the service) on request. If"
                    + " this PID is known, it will be returned together with the timestamps of"
                    + " creation and modification executed on this PID by this service.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "If the PID is known and its information was returned.",
                            content = @Content(schema = @Schema(implementation = KnownPid.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If the PID is unknown.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    ),
                    @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            }
    )
    @GetMapping(path = "known-pid/**")
    ResponseEntity<KnownPid> findByPid(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Returns all known PIDs, limited by the given page size and number.
     * Several filtering criteria are also available.
     * <p>
     * Known PIDs are defined as being stored in a local store. This store is not a
     * cache! Instead, the service remembers every PID which it created (and
     * resolved, depending on the configuration parameter `pit.storage.strategy` of
     * the service) on request.
     *
     * @param createdAfter   defines the earliest date for the creation timestamp.
     * @param createdBefore  defines the latest date for the creation timestamp.
     * @param modifiedAfter  defines the earliest date for the modification
     *                       timestamp.
     * @param modifiedBefore defines the latest date for the modification timestamp.
     * @param pageable       defines page size and page to navigate through large
     *                       lists.
     * @return the PIDs matching all given contraints.
     */
    @Operation(
            summary = "Returns all known PIDs. Supports paging, filtering criteria, and different formats.",
            description = "Returns all known PIDs, limited by the given page size and number. "
                    + "Several filtering criteria are also available. Known PIDs are defined as "
                    + "being stored in a local store. This store is not a cache! Instead, the "
                    + "service remembers every PID which it created (and resolved, depending on "
                    + "the configuration parameter `pit.storage.strategy` of the service) on "
                    + "request. Use the Accept header to adjust the format.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "If the request was valid. May return an empty list.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KnownPid.class)))
                    ),
                    @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            }
    )
    @GetMapping(path = "known-pid")
    @PageableAsQueryParam
    ResponseEntity<List<KnownPid>> findAll(
            @Parameter(name = "created_after", description = "The UTC time of the earliest creation timestamp of a returned PID.", required = false)
            @RequestParam(name = "created_after", required = false)
            Instant createdAfter,

            @Parameter(name = "created_before", description = "The UTC time of the latest creation timestamp of a returned PID.", required = false)
            @RequestParam(name = "created_before", required = false)
            Instant createdBefore,

            @Parameter(name = "modified_after", description = "The UTC time of the earliest modification timestamp of a returned PID.", required = false)
            @RequestParam(name = "modified_after", required = false)
            Instant modifiedAfter,

            @Parameter(name = "modified_before", description = "The UTC time of the latest modification timestamp of a returned PID.", required = false)
            @RequestParam(name = "modified_before", required = false)
            Instant modifiedBefore,

            @Parameter(hidden = true)
            @PageableDefault(sort = {"modified"}, direction = Sort.Direction.ASC)
            Pageable pageable,

            WebRequest request,

            HttpServletResponse response,

            UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Like findAll, but the return value is formatted for the tabulator
     * javascript library.
     *
     * @param createdAfter   defines the earliest date for the creation timestamp.
     * @param createdBefore  defines the latest date for the creation timestamp.
     * @param modifiedAfter  defines the earliest date for the modification
     *                       timestamp.
     * @param modifiedBefore defines the latest date for the modification timestamp.
     * @param pageable       defines page size and page to navigate through large
     *                       lists.
     * @return the PIDs matching all given contraints.
     */
    @Operation(
            summary = "Returns all known PIDs. Supports paging, filtering criteria, and different formats.",
            description = "Returns all known PIDs, limited by the given page size and number. "
                    + "Several filtering criteria are also available. Known PIDs are defined as "
                    + "being stored in a local store. This store is not a cache! Instead, the "
                    + "service remembers every PID which it created (and resolved, depending on "
                    + "the configuration parameter `pit.storage.strategy` of the service) on "
                    + "request. Use the Accept header to adjust the format.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "If the request was valid. May return an empty list.",
                            content = @Content(schema = @Schema(implementation = TabulatorPaginationFormat.class))
                    ),
                    @ApiResponse(responseCode = "500", description = "Server error. See body for details.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            }
    )
    @GetMapping(path = "known-pid", produces = {"application/tabulator+json"}, headers = "Accept=application/tabulator+json")
    @PageableAsQueryParam
    ResponseEntity<TabulatorPaginationFormat<KnownPid>> findAllForTabular(
            @Parameter(name = "created_after", description = "The UTC time of the earliest creation timestamp of a returned PID.", required = false)
            @RequestParam(name = "created_after", required = false)
            Instant createdAfter,

            @Parameter(name = "created_before", description = "The UTC time of the latest creation timestamp of a returned PID.", required = false)
            @RequestParam(name = "created_before", required = false)
            Instant createdBefore,

            @Parameter(name = "modified_after", description = "The UTC time of the earliest modification timestamp of a returned PID.", required = false)
            @RequestParam(name = "modified_after", required = false)
            Instant modifiedAfter,

            @Parameter(name = "modified_before", description = "The UTC time of the latest modification timestamp of a returned PID.", required = false)
            @RequestParam(name = "modified_before", required = false)
            Instant modifiedBefore,

            @Parameter(hidden = true)
            @PageableDefault(sort = {"modified"}, direction = Sort.Direction.ASC)
            Pageable pageable,

            WebRequest request,

            HttpServletResponse response,

            UriComponentsBuilder uriBuilder
    ) throws IOException;
}