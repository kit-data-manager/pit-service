package edu.kit.datamanager.pit.web.impl;

import edu.kit.datamanager.exceptions.CustomInternalServerError;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import edu.kit.datamanager.pit.common.PidAlreadyExistsException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.common.TypeNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.configuration.PidGenerationProperties;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticRepository;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticWrapper;
import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.util.TypeValidationUtils;
import edu.kit.datamanager.pit.web.ITypingRestResource;
import edu.kit.datamanager.pit.web.TabulatorPaginationFormat;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.entities.messaging.PidRecordMessage;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/v1/pit")
@Schema(description = "PID Information Types API")
public class TypingRESTResourceImpl implements ITypingRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(TypingRESTResourceImpl.class);

    @Autowired
    private ApplicationProperties applicationProps;

    @Autowired
    protected ITypingService typingService;

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
    public ResponseEntity<String> isPidMatchingProfile(String identifier,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        LOG.trace("Performing isPidMatchingProfile({}).", identifier);

        String profileId = getContentPathFromRequest("profile", request);
        LOG.trace(
                "Validating PID record with identifier {} against profile with identifier {} from request path.",
                identifier,
                profileId
        );

        PIDRecord pidRecord = this.typingService.queryAllProperties(identifier);
        this.saveToElastic(pidRecord);
        if (this.applicationProps.getStorageStrategy().storesResolved()) {
            this.storeLocally(identifier, false);
        }

        if (typingService.conformsToType(identifier, profileId)) {
            LOG.trace("PID record with identifier {} is matching profile with identifier {}.", identifier, profileId);
            return ResponseEntity.status(200).build();
        }
        LOG.error("PID record with identifier {} is NOT matching profile with identifier {}.", identifier, profileId);
        throw new RecordValidationException(identifier,
                "Record with identifier " + identifier + " not matching profile with identifier " + profileId + ".");
    }

    @Override
    public ResponseEntity<String> isResourceMatchingType(String identifier,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {

        LOG.trace("Performing isResourceMatchingType({}).", identifier);
        String typeId = getContentPathFromRequest("type", request);
        LOG.trace("Obtaining type definition for identifier {}.", typeId);
        TypeDefinition typeDef = typingService.describeType(typeId);

        if (typeDef == null) {
            LOG.error("No definition found for identifier {}.", typeId);
            throw new TypeNotFoundException(typeId);
        }

        LOG.trace("Reading PID record for identifier {}.", identifier);
        PIDRecord record = typingService.queryAllProperties(identifier);
        LOG.trace("Validating PID record with identifier {} against type with id {} from request path.", identifier,
                typeId);
        if (TypeValidationUtils.isValid(record, typeDef)) {
            LOG.trace("PID record with identifier {} is matching type with identifier {}.", identifier, typeId);
            this.saveToElastic(record);
            if (this.applicationProps.getStorageStrategy().storesResolved()) {
                this.storeLocally(identifier, false);
            }
            return ResponseEntity.ok().build();
        }

        LOG.error("PID record with identifier {} is NOT matching type with identifier {}.", identifier, typeId);
        throw new RecordValidationException(identifier,
                "Record with identifier " + identifier + " not matching type with identifier " + typeId + ".");
    }

    @Override
    public ResponseEntity<TypeDefinition> getProfile(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        String profileId = getContentPathFromRequest("profile", request);

        // read profile from type registry
        TypeDefinition profileDef = typingService.describeType(profileId);
        if (profileDef == null) {
            LOG.error("No definition found for identifier {}.", profileId);
            throw new TypeNotFoundException(profileId);
        }
        return ResponseEntity.status(HttpStatus.FOUND.value()).body(profileDef);
    }

    @Override
    public ResponseEntity<PIDRecord> createPID(
            PIDRecord record,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        LOG.info("Creating PID");

        setPid(record);
        this.typingService.validate(record);
        String pid = this.typingService.registerPID(record);
        // store result locally
        if (applicationProps.getStorageStrategy().storesModified()) {
            storeLocally(pid, true);
        }
        // distribute to other services
        record.setPid(pid);
        PidRecordMessage message = PidRecordMessage.creation(
                pid,
                "", // TODO parameter is depricated and will be removed soon.
                AuthenticationHelper.getPrincipal(),
                ControllerUtils.getLocalHostname());
        try {
            this.messagingService.send(message);
        } catch (Exception e) {
            LOG.error("Could not notify messaging service about the following message: {}", message.toString());
        }
        this.saveToElastic(record);
        return ResponseEntity.status(HttpStatus.CREATED.value()).eTag(quotedEtag(record)).body(record);
    }

    private void setPid(PIDRecord pidRecord) throws IOException {
        boolean hasCustomPid = pidRecord.getPid() != null && !pidRecord.getPid().isBlank();
        boolean allowsCustomPids = pidGenerationProperties.isCustomClientPidsEnabled();

        if (allowsCustomPids && hasCustomPid) {
            // in this only case, we do not have to generate a PID
            // but we have to check if the PID is already registered and return an error if so
            String prefix = this.typingService.getPrefix().orElseThrow(() -> new IOException("No prefix configured."));
            String maybeSuffix = pidRecord.getPid();
            String pid = PidSuffix.asPrefixedChecked(maybeSuffix, prefix);
            boolean isRegisteredPid = this.typingService.isIdentifierRegistered(pid);
            if (isRegisteredPid) {
                throw new PidAlreadyExistsException(pidRecord.getPid());
            }
        } else {
            // In all other (usual) cases, we have to generate a PID.
            // We store only the suffix in the pid field.
            // The registration at the PID service will preprend the prefix.

            Stream<PidSuffix> suffixStream = suffixGenerator.infiniteStream();
            Optional<PidSuffix> maybeSuffix = Streams.stream(suffixStream)
                    // The Streams.stream gives us a failible stream, so we can throw an exception
                    .filter(suffix -> !this.typingService.isIdentifierRegistered(suffix))
                    .stream()  // back to normal java streams
                    .findFirst();  // as the stream is infinite, we should always find a prefix.
            PidSuffix suffix = maybeSuffix.orElseThrow(() -> new IOException("Could not generate PID suffix."));
            pidRecord.setPid(suffix.get());
        }
    }

    @Override
    public ResponseEntity<PIDRecord> updatePID(
            PIDRecord record,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        // PID validation
        String pid = getContentPathFromRequest("pid", request);
        String pid_internal = record.getPid();
        if (pid_internal != null && !pid_internal.isEmpty() && !pid.equals(pid_internal)) {
            throw new RecordValidationException(
                pid,
                "PID in record was given, but it was not the same as the PID in the URL. Ignore request, assuming this was not intended.");
        }
        
        PIDRecord existingRecord = this.typingService.queryAllProperties(pid);
        if (existingRecord == null) {
            throw new PidNotFoundException(pid);
        }
        // throws exception (HTTP 412) if check fails.
        ControllerUtils.checkEtag(request, existingRecord);

        // record validation
        record.setPid(pid);
        this.typingService.validate(record);

        // update and send message
        if (this.typingService.updatePID(record)) {
            // store pid locally
            if (applicationProps.getStorageStrategy().storesModified()) {
                storeLocally(record.getPid(), true);
            }
            // distribute pid to other services
            PidRecordMessage message = PidRecordMessage.update(
                    pid,
                    "", // TODO parameter is depricated and will be removed soon.
                    AuthenticationHelper.getPrincipal(),
                    ControllerUtils.getLocalHostname());
            this.messagingService.send(message);
            this.saveToElastic(record);
            return ResponseEntity.ok().eTag(quotedEtag(record)).body(record);
        } else {
            throw new PidNotFoundException(pid);
        }
    }

    @Override
    public ResponseEntity<String> isPidRegistered(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        String pid = getContentPathFromRequest("pid", request);
        LOG.trace("Obtained PID {} from request.", pid);

        PIDRecord pidRecord = typingService.queryAllProperties(pid);
        if (pidRecord != null) {
            LOG.trace("PID successfully checked.");
            if (applicationProps.getStorageStrategy().storesResolved()) {
                this.storeLocally(pid, false);
            }
            this.saveToElastic(pidRecord);
            return ResponseEntity.ok().body("PID is registered.");
        } else {
            LOG.error("PID {} not found at configured identifier system.", pid);
            throw new PidNotFoundException("Identifier with value " + pid + " not found.");
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
                WebRequest.SCOPE_REQUEST);
        if (requestedUri == null) {
            throw new CustomInternalServerError("Unable to obtain request URI.");
        }
        return requestedUri.substring(requestedUri.indexOf(lastPathElement + "/") + (lastPathElement + "/").length());
    }

    @Override
    public ResponseEntity<PIDRecord> getRecord(
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) throws IOException {
        String pid = getContentPathFromRequest("pid", request);
        PIDRecord rec = this.typingService.queryAllProperties(pid);
        if (applicationProps.getStorageStrategy().storesResolved()) {
            storeLocally(pid, false);
        }
        this.saveToElastic(rec);
        return ResponseEntity.ok().eTag(quotedEtag(rec)).body(rec);
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
            UriComponentsBuilder uriBuilder) throws IOException
    {
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
            UriComponentsBuilder uriBuilder) throws IOException
    {
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
