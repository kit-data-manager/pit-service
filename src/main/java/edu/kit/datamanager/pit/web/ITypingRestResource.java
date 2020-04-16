/*
 * Copyright 2020 Karlsruhe Institute of Technology.
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
public interface ITypingRestResource {

    /**
     * Check if a certain resource with a given PID is matching a profile. The
     * profile is identified by its PID provided as path segment(s), whereas the
     * resource is identified via its PID as request param.
     *
     * @param identifier The resource identifier
     *
     * @return either 200 or 404, indicating whether the PID is registered or
     * not registered
     * @throws IOException
     */
    @RequestMapping(path = "/profile/**", method = RequestMethod.HEAD)
    @Operation(summary = "PID matching profile?",
            description = "Check if the PID record accessible via the provided identifier is matching the profile provided "
            + "as the last path segment(s). The check only includes the test for mandatory fields according to the profile. For "
            + "in-depth tests endpoint /type/{identifier} should be used.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity isPidMatchingProfile(@RequestParam("identifier") String identifier,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException;

    /**
     * Check if a certain resource with a given PID is matching a type. The type
     * is identified by its PID provided as path segment(s), whereas the
     * resource is identified via its PID as request param.
     *
     * @param identifier The resource identifier
     *
     * @return either 200 or 404, indicating whether the PID is registered or
     * not registered
     * @throws IOException
     */
    @RequestMapping(path = "/type/**", method = RequestMethod.HEAD)
    @Operation(summary = "Resource matching type?",
            description = "Check if the resource accessible via the provided identifier is matching the "
            + "type provided as the last path segment(s). The check includes the test if all mandatory properties are in the record as well as "
            + "an in-depth tests of the single elements for matching the sub-type's schema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity isResourceMatchingType(@RequestParam("identifier") String identifier,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException;

    /**
     * Get a profile by its identifier presented by the last path segment(s).
     *
     * @return either 200 or 404, indicating whether the profile is registered
     * or not registered
     *
     * @throws IOException
     */
    @RequestMapping(path = "/profile/**", method = RequestMethod.GET)
    @Operation(summary = "Get a profile", description = "Obtain the profile identified by the PID provided as the last path segment(s).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity getProfile(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException;

    /**
     * Create a new PID using the record information provided in the request
     * body. The record is expected to contain the identifier of the matching
     * profile. Before creating the record, the record information will be
     * validated against the profile.
     *
     * @param record The PID record.
     *
     * @return either 200 or 404, indicating whether the profile is registered
     * or not registered
     *
     * @throws IOException
     */
    @RequestMapping(path = "/pid/", method = RequestMethod.POST)
    @Operation(summary = "Create a new PID record", description = "Create a new PID record using the record information from the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity createPID(@RequestBody final PIDRecord record,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException;

    /**
     * Create a new PID using the record information provided in the request
     * body. The record is expected to contain the identifier of the matching
     * profile. Before creating the record, the record information will be
     * validated against the profile.
     *
     * @param record The PID record.
     *
     * @return either 200 or 404, indicating whether the profile is registered
     * or not registered
     *
     * @throws IOException
     */
    @RequestMapping(path = "/pid/**", method = RequestMethod.PUT)
    @Operation(summary = "Update an existing PID record", description = "Update an existing PID record using the record information from the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity updatePID(@RequestBody final PIDRecord record,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException;

    /**
     * Check if a certain PID provided as path segment(s) exist.
     *
     * @return either 200 or 404, indicating whether the PID is registered or
     * not registered
     *
     * @throws IOException
     */
    @RequestMapping(path = "/pid/**", method = RequestMethod.HEAD)
    @Operation(summary = "Check if PID exists.", description = "Check if the PID with the idenfifier provided as the last path segment(s) exists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity isPidRegistered(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException;

    ////////////////
    ////////////////
    ////////////////
    ////////////////
    ////////////////
    /**
     * Simple ping method for testing (check whether the API is running etc.).
     * Not part of the official interface description.
     *
     * @return responds with 200 OK and a "Hello World" message in the body.
     */
//    @GetMapping(path = "/ping")
//    @Operation(summary = "Ping service", description = "Determine if service is running")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Service available"),})
//    public ResponseEntity simplePing();
//
//    /**
//     * Generic resolution method to read PID records, property or type
//     * definitions. Optionally implemented method. May be slower than the
//     * specialized methods due to an increased number of back-end requests.
//     *
//     * @param identifier an identifier string
//     * @return depending on the nature of the identified entity, the result can
//     * be a PID record, a property or a type definition.
//     * @throws IOException
//     */
//    @RequestMapping(path = "/generic/{identifier}", method = RequestMethod.GET)
//    @Operation(summary = "Lookup by identifier", description = "More notes about this method")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveGenericPID(
//            @Parameter(description = "ID of entity") @PathVariable("identifier") String identifier)
//            throws IOException;
//
//    /**
//     * Similar to {@link #resolveGenericPID(String)} but supports native slashes
//     * in the identifier path.
//     *
//     * @see #resolveGenericPID(String)
//     */
//    @RequestMapping(path = "/generic/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Lookup by identifier", description = "More notes about this method")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveGenericPID(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * Simple HEAD method to check whether a particular pid is registered.
//     *
//     * @param identifier an identifier string
//     * @return either 200 or 404, indicating whether the PID is registered or
//     * not registered
//     * @throws IOException
//     */
//    @RequestMapping(path = "/pid/{identifier}", method = RequestMethod.HEAD)
//    @Operation(summary = "Identifier registered?", description = "Check to see if identifier is registered")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity isPidRegistered(@PathVariable("identifier") String identifier) throws IOException;
//
//    /**
//     * Similar to {@link #isPidRegistered(String)} but supports native slashes
//     * in the identifier path.
//     *
//     * @see #isPidRegistered(String)
//     */
//    @RequestMapping(path = "/pid/{prefix}/{suffix}", method = RequestMethod.HEAD)
//    @Operation(summary = "Identifier registered?", description = "Check to see if identifier is registered")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity isPidRegistered(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * Queries what kind of entity an identifier will point to (generic object,
//     * property, type, ...). See {@link EntityClass} for possible return values.
//     *
//     * @param identifier full identifier name
//     * @return a simple JSON object with the kind of entity the identifier
//     * points to. See {@link EntityClass} for details.
//     * @throws IOException
//     * @see rdapit.pitservice.EntityClass
//     */
//    @RequestMapping(path = "/peek/{identifier}", method = RequestMethod.GET)
//    @Operation(summary = "Class of identifier's entity?", description = "Get the class of the identified entity")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found")
//    })
//    public ResponseEntity peekIdentifier(@PathVariable("identifier") String identifier) throws IOException;
//
//    /**
//     * Similar to {@link #peekIdentifier(String)} but supports native slashes in
//     * the identifier path.
//     *
//     * @see #peekIdentifier(String)
//     */
//    @RequestMapping(path = "/peek/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Class of identifier's entity?", description = "Get the class of the identified entity")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found")
//    })
//    public ResponseEntity peekIdentifier(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * Sophisticated GET method to return all or some properties of an
//     * identifier.
//     *
//     * @param identifier full identifier name
//     * @param propertyIdentifier Optional. Cannot be used in combination with
//     * the type parameter. If given, the method returns only the value of the
//     * single property. The identifier must be registered for a property in the
//     * type registry. The method will return 404 if the PID exists but does not
//     * carry the given property.
//     * @param typeIdentifiers Optional. Cannot be used in combination with the
//     * property parameter. If given, the method will return all properties
//     * (mandatory and optional) that are specified in the given type(s) and
//     * listed in the identifier's record. The type parameter must be a list of
//     * type identifiers available from the registry. If an identifier is not
//     * known in the registry, the method will return 404. The result will also
//     * include a boolean value <i>typeConformance</i> that is only true if all
//     * mandatory properties of the type are present in the PID record.
//     * @param includePropertyNames Optional. If set to true, the method will
//     * also provide property names in addition to identifiers. Note that this is
//     * more expensive due to extra requests sent to the type registry.
//     * @return if the request is processed properly, the method will return 200
//     * OK and a JSON object that contains a map of property identifiers to
//     * property names (which may be empty) and values. It may also contain
//     * optional meta information, e.g. conformance indications. The method will
//     * return 404 if the identifier is not known.
//     * @throws IOException on communication errors with identifier system or
//     * type registry
//     * @throws InconsistentRecordsException if records in the identifier system
//     * and/or type registry are inconsistent, e.g. use property or type
//     * identifiers that are not registered
//     */
//    @RequestMapping(path = "/pid/{identifier}", method = RequestMethod.GET)
//    @Operation(summary = "Get associated attributes", description = "Get attributes associated with given identifier")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "400", description = "Bad request"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolvePID(@PathVariable("identifier") String identifier,
//            @RequestParam(value = "filter_by_property", defaultValue = "") String propertyIdentifier,
//            @RequestParam(value = "filter_by_type", defaultValue = "") List<String> typeIdentifiers,
//            @RequestParam(value = "include_property_names", defaultValue = "false") boolean includePropertyNames) throws IOException, InconsistentRecordsException;
//
//    /**
//     * Similar to {@link #resolvePID(String, String, List, boolean)} but
//     * supports native slashes in the identifier path.
//     *
//     * @see #resolvePID(String, String, List, boolean)
//     */
//    @RequestMapping(path = "/pid/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Get associated attributes", description = "Get attributes associated with given identifier")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "400", description = "Bad request"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolvePID(@PathVariable("prefix") String identifierPrefix, @PathVariable("suffix") String identifierSuffix,
//            @RequestParam(value = "filter_by_property", defaultValue = "") String propertyIdentifier, @RequestParam("filter_by_type") List<String> typeIdentifiers,
//            @RequestParam(value = "include_property_names", defaultValue = "false") boolean includePropertyNames) throws IOException, InconsistentRecordsException;
//
//    /**
//     * GET method to read the definition of a property from the type registry.
//     *
//     * @param identifier the property identifier
//     * @return a property definition record or 404 if the property is unknown.
//     * @throws IOException
//     */
//    @RequestMapping(path = "/property/{identifier}", method = RequestMethod.GET)
//    @Operation(summary = "Get property definition", description = "Get definition of specified property")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveProperty(@PathVariable("identifier") String identifier) throws IOException;
//
//    /**
//     * Similar to {@link #resolveProperty(String)} but supports native slashes
//     * in the identifier path.
//     *
//     * @see #resolveProperty(String)
//     */
//    @RequestMapping(path = "/property/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Get property definition", description = "Get definition of specified property")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveProperty(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * GET method to read the definition of a type from the type registry.
//     *
//     * @param identifier the type identifier
//     * @return a type definition record or 404 if the type is unknown.
//     * @throws IOException
//     */
//    @RequestMapping(path = "/type/{identifier}", method = RequestMethod.GET)
//    @Operation(summary = "Get type definition", description = "Get definition of specified type")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveType(@PathVariable("identifier") String identifier) throws IOException;
//
//    /**
//     * Similar to {@link #resolveType(String)} but supports native slashes in
//     * the identifier path.
//     *
//     * @see #resolveType(String)
//     */
//    @RequestMapping(path = "/type/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Get type definition", description = "Get definition of specified type")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveType(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * GET method to read the definition of a profile from the type registry.
//     *
//     * @param identifier the profile identifier
//     * @return a profile definition record or 404 if the profile is unknown.
//     * @throws IOException
//     *
//     * Added by Quan (Gabriel) Zhou @ Indiana University Bloomington
//     */
//    @RequestMapping(path = "/profile/{identifier}", method = RequestMethod.GET)
//
//    @Operation(summary = "Get profile definition", description = "Get definition of specified profile")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveProfile(@PathVariable("identifier") String identifier) throws IOException;
//
//    /**
//     * Similar to {@link #resolveProfile(String)} but supports native slashes in
//     * the identifier path.
//     *
//     * @see #resolveProfile(String)
//     */
//    @RequestMapping(path = "/profile/{prefix}/{suffix}", method = RequestMethod.GET)
//    @Operation(summary = "Get profile definition", description = "Get definition of specified profile")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "404", description = "Not found")
//    })
//    public ResponseEntity resolveProfile(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix) throws IOException;
//
//    /**
//     * Generic POST method to create new identifiers. The method determines an
//     * identifier name automatically, based on a purely random (version 4) UUID.
//     *
//     * @param properties a map from string to string, mapping property
//     * identifiers to values.
//     * @return a simple string with the newly created PID name.
//     */
//    @RequestMapping(path = "/pid", method = RequestMethod.POST)
//    @Operation(summary = "Get type definition", description = "Get definition of specified type")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Found"),
//        @ApiResponse(responseCode = "500", description = "Server error")
//    })
//    public ResponseEntity registerPID(Map<String, String> properties);
//
//    /**
//     * DELETE method to delete identifiers. Testing purposes only! Not part of
//     * the official specification.
//     *
//     * @param identifier full identifier name
//     * @return 200 or 404
//     */
//    @RequestMapping(path = "/pid/{identifier}", method = RequestMethod.DELETE)
//    public ResponseEntity deletePID(@PathVariable("identifier") String identifier);
//
//    /**
//     * Similar to {@link #deletePID(String)} but supports native slashes in the
//     * identifier path.
//     *
//     * @see #deletePID(String)
//     */
//    @RequestMapping(path = "/pid/{prefix}/{suffix}", method = RequestMethod.DELETE)
//    public ResponseEntity deletePID(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix);
}
