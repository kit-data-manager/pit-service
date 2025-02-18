/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
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

package edu.kit.datamanager.pit.web.impl;

import edu.kit.datamanager.entities.messaging.PidRecordMessage;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.PidGenerationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticRepository;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticWrapper;
import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.resolver.Resolver;
import edu.kit.datamanager.pit.web.ITypingRestResource;
import edu.kit.datamanager.pit.web.TabulatorPaginationFormat;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.stream.Streams;
import org.apache.http.client.cache.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/api/v1/pit")
@Schema(description = "PID Information Types API")
public class TypingRESTResourceImpl implements ITypingRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(TypingRESTResourceImpl.class);
    @Autowired
    protected ITypingService typingService;
    @Autowired
    protected Resolver resolver;
    @Autowired
    private ApplicationProperties applicationProps;
    @Autowired
    private IMessagingService messagingService;

    @Autowired
    private KnownPidsDao localPidStorage;

    @Autowired
    private Optional<PidRecordElasticRepository> elastic;

    @Autowired
    private PidSuffixGenerator suffixGenerator;

    @Autowired
    private PidGenerationProperties pidGenerationProperties;

    public TypingRESTResourceImpl() {
        super();
    }

    @Override
    public ResponseEntity<List<PIDRecord>> createPIDs(
            List<PIDRecord> rec,
            boolean dryrun,
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder
    ) throws IOException, RecordValidationException, ExternalServiceException {
        Instant startTime = Instant.now();
        LOG.info("Creating PIDs for {} records.", rec.size());
        String prefix = this.typingService.getPrefix().orElseThrow(() -> new IOException("No prefix configured."));

        // Generate a map between temporary (user-defined) PIDs and final PIDs (generated)
        Map<String, String> pidMappings = generatePIDMapping(rec, dryrun);
        Instant mappingTime = Instant.now();

        // Apply the mappings to the records and validate them
        List<PIDRecord> validatedRecords = applyMappingsToRecordsAndValidate(rec, pidMappings, prefix);
        Instant validationTime = Instant.now();

        if (dryrun) {
            // dryrun only does validation. Stop now and return as we would later on.
            LOG.info("Time taken for dryrun: {} ms", ChronoUnit.MILLIS.between(startTime, validationTime));
            LOG.info("-- Time taken for mapping: {} ms", ChronoUnit.MILLIS.between(startTime, mappingTime));
            LOG.info("-- Time taken for validation: {} ms", ChronoUnit.MILLIS.between(mappingTime, validationTime));
            LOG.info("Dryrun finished. Returning validated records for {} records.", validatedRecords.size());
            return ResponseEntity.status(HttpStatus.OK).body(validatedRecords);
        }

        List<PIDRecord> failedRecords = new ArrayList<>();
        // register the records
        validatedRecords.forEach(pidRecord -> {
            try {
                // register the PID
                String pid = this.typingService.registerPid(pidRecord);
                pidRecord.setPid(pid);

                // store pid locally in accordance with the storage strategy
                if (applicationProps.getStorageStrategy().storesModified()) {
                    storeLocally(pid, true);
                }

                // distribute pid creation event to other services
                PidRecordMessage message = PidRecordMessage.creation(
                        pid,
                        "", // TODO parameter is deprecated and will be removed soon.
                        AuthenticationHelper.getPrincipal(),
                        ControllerUtils.getLocalHostname());
                try {
                    this.messagingService.send(message);
                } catch (Exception e) {
                    LOG.error("Could not notify messaging service about the following message: {}", message);
                }

                // save the record to elastic
                this.saveToElastic(pidRecord);
            } catch (Exception e) {
                LOG.error("Could not register PID for record {}. Error: {}", pidRecord, e.getMessage());
                failedRecords.add(pidRecord);
                validatedRecords.remove(pidRecord);
            }
        });

        Instant endTime = Instant.now();

        // return the created records
        LOG.info("Total time taken: {} ms", ChronoUnit.MILLIS.between(startTime, endTime));
        LOG.info("-- Time taken for mapping: {} ms", ChronoUnit.MILLIS.between(startTime, mappingTime));
        LOG.info("-- Time taken for validation: {} ms", ChronoUnit.MILLIS.between(mappingTime, validationTime));
        LOG.info("-- Time taken for registration: {} ms", ChronoUnit.MILLIS.between(validationTime, endTime));

        if (!failedRecords.isEmpty()) {
            for (PIDRecord successfulRecord : validatedRecords) { // rollback the successful records
                try {
                    LOG.debug("Rolling back PID creation for record with PID {}.", successfulRecord.getPid());
                    this.typingService.deletePid(successfulRecord.getPid());
                } catch (Exception e) {
                    LOG.error("Could not rollback PID creation for record with PID {}. Error: {}", successfulRecord.getPid(), e.getMessage());
                }
            }

            LOG.info("Creation finished. Returning validated records for {} records. {} records failed to be created.", validatedRecords.size(), failedRecords.size());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failedRecords);
        } else {
            LOG.info("Creation finished. Returning validated records for {} records.", validatedRecords.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(validatedRecords);
        }
    }

    /**
     * This method generates a mapping between user-provided "fantasy" PIDs and real PIDs.
     *
     * @param rec    the list of records produced by the user
     * @param dryrun whether the operation is a dryrun or not
     * @return a map between the user-provided PIDs (key) and the real PIDs (values)
     * @throws IOException               if the prefix is not configured
     * @throws RecordValidationException if the same internal PID is used for multiple records
     * @throws ExternalServiceException  if the PID generation fails
     */
    private Map<String, String> generatePIDMapping(List<PIDRecord> rec, boolean dryrun) throws IOException, RecordValidationException, ExternalServiceException {
        Map<String, String> pidMappings = new HashMap<>();
        for (PIDRecord pidRecord : rec) {
            String internalPID = pidRecord.getPid(); // the internal PID is the one given by the user
            if (!internalPID.isBlank() && pidMappings.containsKey(internalPID)) { // check if the internal PID was already used
                // This internal PID was already used by some other record in the same request.
                throw new RecordValidationException(pidRecord, "The PID " + internalPID + " was used for multiple records in the same request.");
            }

            pidRecord.setPid(""); // clear the PID field in the record
            if (dryrun) { // if it is a dryrun, we set the PID to a temporary value
                pidRecord.setPid("dryrun_" + pidMappings.size());
            } else {
                setPid(pidRecord); // otherwise, we generate a real PID
            }
            pidMappings.put(internalPID, pidRecord.getPid()); // store the mapping between the internal and real PID
        }
        return pidMappings;
    }

    /**
     * This method applies the mappings between temporary PIDs and real PIDs to the records and validates them.
     *
     * @param rec         the list of records produced by the user
     * @param pidMappings the map between the user-provided PIDs (key) and the real PIDs (values)
     * @param prefix      the prefix to be used for the real PIDs
     * @return the list of validated records
     * @throws RecordValidationException as a possible validation outcome
     * @throws ExternalServiceException  as a possible validation outcome
     */
    private List<PIDRecord> applyMappingsToRecordsAndValidate(List<PIDRecord> rec, Map<String, String> pidMappings, String prefix) throws RecordValidationException, ExternalServiceException {
        List<PIDRecord> validatedRecords = new ArrayList<>();
        for (PIDRecord pidRecord : rec) {

            // use this map to replace all temporary PIDs in the record values with their corresponding real PIDs
            pidRecord.getEntries().values().stream() // get all values of the record
                    .flatMap(List::stream) // flatten the list of values
                    .filter(entry -> entry.getValue() != null) // Filter out null values
                    .filter(entry -> pidMappings.containsKey(entry.getValue())) // replace only if the value (aka. "fantasy PID") is a key in the map
                    .peek(entry -> LOG.debug("Found reference. Replacing {} with {}.", entry.getValue(), prefix + pidMappings.get(entry.getValue()))) // log the replacement
                    .forEach(entry -> entry.setValue(prefix + pidMappings.get(entry.getValue()))); // replace the value with the real PID according to the map

            // validate the record
            this.typingService.validate(pidRecord);

            // store the record
            validatedRecords.add(pidRecord);
            LOG.debug("Record {} is valid.", pidRecord);
        }
        return validatedRecords;
    }

    @Override
    public ResponseEntity<PIDRecord> createPID(
            PIDRecord pidRecord,
            boolean dryrun,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException {
        LOG.info("Creating PID");

        if (dryrun) {
            pidRecord.setPid("dryrun");
        } else {
            setPid(pidRecord);
        }

        this.typingService.validate(pidRecord);

        if (dryrun) {
            // dryrun only does validation. Stop now and return as we would later on.
            return ResponseEntity.status(HttpStatus.OK).eTag(quotedEtag(pidRecord)).body(pidRecord);
        }

        String pid = this.typingService.registerPid(pidRecord);
        pidRecord.setPid(pid);

        if (applicationProps.getStorageStrategy().storesModified()) {
            storeLocally(pid, true);
        }
        PidRecordMessage message = PidRecordMessage.creation(
                pid,
                "", // TODO parameter is deprecated and will be removed soon.
                AuthenticationHelper.getPrincipal(),
                ControllerUtils.getLocalHostname());
        try {
            this.messagingService.send(message);
        } catch (Exception e) {
            LOG.error("Could not notify messaging service about the following message: {}", message);
        }
        this.saveToElastic(pidRecord);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(quotedEtag(pidRecord)).body(pidRecord);
    }

    private boolean hasPid(PIDRecord pidRecord) {
        return pidRecord.getPid() != null && !pidRecord.getPid().isBlank();
    }

    private void setPid(PIDRecord pidRecord) throws IOException {
        boolean hasCustomPid = hasPid(pidRecord);
        boolean allowsCustomPids = pidGenerationProperties.isCustomClientPidsEnabled();

        if (allowsCustomPids && hasCustomPid) {
            // in this only case, we do not have to generate a PID
            // but we have to check if the PID is already registered and return an error if so
            String prefix = this.typingService.getPrefix()
                    .orElseThrow(() -> new InvalidConfigException("No prefix configured."));
            String maybeSuffix = pidRecord.getPid();
            String pid = PidSuffix.asPrefixedChecked(maybeSuffix, prefix);
            boolean isRegisteredPid = this.typingService.isPidRegistered(pid);
            if (isRegisteredPid) {
                throw new PidAlreadyExistsException(pidRecord.getPid());
            }
        } else {
            // In all other (usual) cases, we have to generate a PID.
            // We store only the suffix in the pid field.
            // The registration at the PID service will preprend the prefix.

            Stream<PidSuffix> suffixStream = suffixGenerator.infiniteStream();
            Optional<PidSuffix> maybeSuffix = Streams.failableStream(suffixStream)
                    // With failable streams, we can throw exceptions.
                    .filter(suffix -> !this.typingService.isPidRegistered(suffix))
                    .stream()  // back to normal java streams
                    .findFirst();  // as the stream is infinite, we should always find a prefix.
            PidSuffix suffix = maybeSuffix
                    .orElseThrow(() -> new ExternalServiceException("Could not generate PID suffix which did not exist yet."));
            pidRecord.setPid(suffix.get());
        }
    }

    @Override
    public ResponseEntity<PIDRecord> updatePID(
            PIDRecord pidRecord,
            boolean dryrun,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException {
        // PID validation
        String pid = getContentPathFromRequest("pid", request);
        String pidInternal = pidRecord.getPid();
        if (hasPid(pidRecord) && !pid.equals(pidInternal)) {
            throw new RecordValidationException(
                    pidRecord,
                    "Optional PID in record is given (%s), but it was not the same as the PID in the URL (%s). Ignore request, assuming this was not intended.".formatted(pidInternal, pid));
        }

        PIDRecord existingRecord = this.resolver.resolve(pid);
        if (existingRecord == null) {
            throw new PidNotFoundException(pid);
        }

        // record validation
        pidRecord.setPid(pid);
        this.typingService.validate(pidRecord);

        // throws exception (HTTP 412) if check fails.
        ControllerUtils.checkEtag(request, existingRecord);

        if (dryrun) {
            // dryrun only does validation. Stop now and return as we would later on.
            return ResponseEntity.ok().eTag(quotedEtag(pidRecord)).body(pidRecord);
        }

        // update and send message
        if (this.typingService.updatePid(pidRecord)) {
            // store pid locally
            if (applicationProps.getStorageStrategy().storesModified()) {
                storeLocally(pidRecord.getPid(), true);
            }
            // distribute pid to other services
            PidRecordMessage message = PidRecordMessage.update(
                    pid,
                    "", // TODO parameter is depricated and will be removed soon.
                    AuthenticationHelper.getPrincipal(),
                    ControllerUtils.getLocalHostname());
            this.messagingService.send(message);
            this.saveToElastic(pidRecord);
            return ResponseEntity.ok().eTag(quotedEtag(pidRecord)).body(pidRecord);
        } else {
            throw new PidNotFoundException(pid);
        }
    }

    /**
     * Stores the PID in a local database.
     *
     * @param pid    the PID
     * @param update if true, updates the modified timestamp if it already exists.
     *               If it does not exist, it will be created with both timestamps
     *               (created and modified) being the same.
     */
    private void storeLocally(String pid, boolean update) {
        Instant now = Instant.now();
        Optional<KnownPid> oldPid = localPidStorage.findByPid(pid);
        if (oldPid.isEmpty()) {
            localPidStorage.saveAndFlush(new KnownPid(pid, now, now));
        } else if (update) {
            KnownPid newPid = oldPid.get();
            newPid.setModified(now);
            localPidStorage.saveAndFlush(newPid);
        }
    }

    private String getContentPathFromRequest(String lastPathElement, WebRequest request) {
        String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (requestedUri == null) {
            throw new CustomInternalServerError("Unable to obtain request URI.");
        }
        return requestedUri.substring(requestedUri.indexOf(lastPathElement + "/") + (lastPathElement + "/").length());
    }

    @Override
    public ResponseEntity<PIDRecord> getRecord(
            boolean validation,

            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder
    ) throws IOException {
        String pid = getContentPathFromRequest("pid", request);
        PIDRecord pidRecord = this.resolver.resolve(pid);
        if (applicationProps.getStorageStrategy().storesResolved()) {
            storeLocally(pid, false);
        }
        this.saveToElastic(pidRecord);
        if (validation) {
            typingService.validate(pidRecord);
        }
        return ResponseEntity.ok().eTag(quotedEtag(pidRecord)).body(pidRecord);
    }

    private void saveToElastic(PIDRecord rec) {
        this.elastic.ifPresent(
                database -> database.save(
                        new PidRecordElasticWrapper(rec, typingService.getOperations())
                )
        );
    }

    @Override
    public ResponseEntity<KnownPid> findByPid(
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder
    ) throws IOException {
        String pid = getContentPathFromRequest("known-pid", request);
        Optional<KnownPid> known = this.localPidStorage.findByPid(pid);
        if (known.isPresent()) {
            return ResponseEntity.ok().body(known.get());
        }
        return ResponseEntity.notFound().build();
    }

    public Page<KnownPid> findAllPage(
            Instant createdAfter,
            Instant createdBefore,
            Instant modifiedAfter,
            Instant modifiedBefore,
            Pageable pageable
    ) {
        final boolean queriesCreated = createdAfter != null || createdBefore != null;
        final boolean queriesModified = modifiedAfter != null || modifiedBefore != null;
        if (queriesCreated && createdAfter == null) {
            createdAfter = Instant.EPOCH;
        }
        if (queriesCreated && createdBefore == null) {
            createdBefore = Instant.now().plus(1, ChronoUnit.DAYS);
        }
        if (queriesModified && modifiedAfter == null) {
            modifiedAfter = Instant.EPOCH;
        }
        if (queriesModified && modifiedBefore == null) {
            modifiedBefore = Instant.now().plus(1, ChronoUnit.DAYS);
        }

        Page<KnownPid> resultCreatedTimestamp = Page.empty();
        Page<KnownPid> resultModifiedTimestamp = Page.empty();
        if (queriesCreated) {
            resultCreatedTimestamp = this.localPidStorage
                    .findDistinctPidsByCreatedBetween(createdAfter, createdBefore, pageable);
        }
        if (queriesModified) {
            resultModifiedTimestamp = this.localPidStorage
                    .findDistinctPidsByModifiedBetween(modifiedAfter, modifiedBefore, pageable);
        }
        if (queriesCreated && queriesModified) {
            final Page<KnownPid> tmp = resultModifiedTimestamp;
            final List<KnownPid> intersection = resultCreatedTimestamp.filter((x) -> tmp.getContent().contains(x)).toList();
            return new PageImpl<>(intersection);
        } else if (queriesCreated) {
            return resultCreatedTimestamp;
        } else if (queriesModified) {
            return resultModifiedTimestamp;
        }
        return new PageImpl<>(this.localPidStorage.findAll());
    }

    @Override
    public ResponseEntity<List<KnownPid>> findAll(
            Instant createdAfter,
            Instant createdBefore,
            Instant modifiedAfter,
            Instant modifiedBefore,
            Pageable pageable,
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder) throws IOException {
        Page<KnownPid> page = this.findAllPage(createdAfter, createdBefore, modifiedAfter, modifiedBefore, pageable);
        response.addHeader(
                HeaderConstants.CONTENT_RANGE,
                ControllerUtils.getContentRangeHeader(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements()));
        return ResponseEntity.ok().body(page.getContent());
    }

    @Override
    public ResponseEntity<TabulatorPaginationFormat<KnownPid>> findAllForTabular(
            Instant createdAfter,
            Instant createdBefore,
            Instant modifiedAfter,
            Instant modifiedBefore,
            Pageable pageable,
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder) throws IOException {
        Page<KnownPid> page = this.findAllPage(createdAfter, createdBefore, modifiedAfter, modifiedBefore, pageable);
        response.addHeader(
                HeaderConstants.CONTENT_RANGE,
                ControllerUtils.getContentRangeHeader(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements()));
        TabulatorPaginationFormat<KnownPid> tabPage = new TabulatorPaginationFormat<>(page);
        return ResponseEntity.ok().body(tabPage);
    }

    private String quotedEtag(PIDRecord pidRecord) {
        return String.format("\"%s\"", pidRecord.getEtag());
    }

}
