package edu.kit.datamanager.pit.pidsystem.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple basis for demonstrations or tests of the service. PIDs will be
 * stored in a HashMap and not stored anywhere else.
 */
@RestController
@RequestMapping(value = "api/inmemoryidentifiersystem")
@Schema(description = "InMemory Identifier Service API as a replacement for real services APIs.")
public class InMemoryIdentifierSystem implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryIdentifierSystem.class);
    private static Map<String, PIDRecord> RECORDS;

    public InMemoryIdentifierSystem() {
        if (InMemoryIdentifierSystem.RECORDS == null) {
            InMemoryIdentifierSystem.RECORDS = new HashMap<>();
        }
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return InMemoryIdentifierSystem.RECORDS.containsKey(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        return InMemoryIdentifierSystem.RECORDS.get(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        return InMemoryIdentifierSystem.RECORDS.get(pid).getPropertyValue(typeDefinition.getIdentifier());
    }

    @Override
    public String registerPID(PIDRecord record) throws IOException {
        record.setPid("tmp/test/" + record.getEntries().hashCode());
        InMemoryIdentifierSystem.RECORDS.put(record.getPid(), record);
        LOG.debug("Registered record with PID: {}", record.getPid());
        return record.getPid();
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = this.queryAllProperties(pid);
        // only return properties listed in the type def
        Set<String> typeProps = typeDefinition.getAllProperties();
        PIDRecord result = new PIDRecord();
        for (String propID : allProps.getPropertyIdentifiers()) {
            if (typeProps.contains(propID)) {
                String[] values = allProps.getPropertyValues(propID);
                for (String value : values) {
                    result.addEntry(propID, "", value);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deletePID(String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    /**
     * Return the record to a given PID.
     *
     * @param pid The PID.
     *
     * @return either 200 or 404, indicating whether the record could be returned or
     *         not.
     *
     */
    @RequestMapping(path = "/resolve", method = RequestMethod.GET)
    @Operation(summary = "Resolve an existing PID record", description = "Resolve a PID and receive the associated record.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public ResponseEntity<PIDRecord> resolve(String pid) {
        try {
            LOG.debug("Try to get record with PID {}", pid);
            PIDRecord record = this.queryAllProperties(pid);
            if (record == null) { LOG.debug("record is null!"); }
            return ResponseEntity.status(200).body(record);
        } catch (IOException e) {
            LOG.debug("Got exception.");
            return ResponseEntity.status(404).build();
        }
    }

    @Override
    public String getResolvingUrl(String pid) {
        String locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).resolve(pid)).toUri()
                .toString();
        locationUri = String.format("%s?pid=%s", locationUri, pid);
        LOG.debug("Returning resolving URL {}", locationUri);
        return locationUri;
    }
}
