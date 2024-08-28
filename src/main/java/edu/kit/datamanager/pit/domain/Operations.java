package edu.kit.datamanager.pit.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.stream.Streams;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import edu.kit.datamanager.pit.pitservice.ITypingService;

/**
 * Simple operations on PID records.
 * 
 * Caches results e.g. for type queries
 */
public class Operations {

    private static final String[] KNOWN_DATE_CREATED = {
        "21.T11148/29f92bd203dd3eaa5a1f",
        "21.T11148/aafd5fb4c7222e2d950a"
    };

    private static final String[] KNOWN_DATE_MODIFIED = {
        "21.T11148/397d831aa3a9d18eb52c"
    };

    private ITypingService typingService;

    public Operations(ITypingService typingService) {
        this.typingService = typingService;
    }

    /**
     * Tries to get the date when a FAIR DO was created from a PID record.
     * 
     * Strategy:
     * - try to get it from known "dateCreated" types
     * - as a fallback, try to get it by its human readable name
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the date, if it could been extracted.
     * @throws IOException on IO errors regarding resolving types.
     */
    public Optional<Date> findDateCreated(PIDRecord pidRecord) throws IOException {
        /* try known types */
        List<String> knownDateTypes = Arrays.asList(Operations.KNOWN_DATE_CREATED);
        Optional<Date> date = knownDateTypes
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
        if (date.isPresent()) {
            return date;
        }

        /* TODO try to find types extending or relating otherwise to known types
         *      (currently not supported by our TypeDefinition) */
        Collection<TypeDefinition> types = new ArrayList<>();
        List<CompletableFuture<?>> futures = Streams
                .stream(pidRecord.getPropertyIdentifiers().stream())
                .filter(attributePid -> this.typingService.isIdentifierRegistered(attributePid))
                .map(attributePid -> {
                    return this.typingService
                            .describeType(attributePid)
                            .thenAcceptAsync(types::add);
                })
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        /*
         * as a last fallback, try find types with human readable names containing
         * "dateCreated" or "createdAt" or "creationDate".
         * 
         * This can be removed as soon as we have some default FAIR DO types new type
         * definitions can refer to (e.g. "extend" them or declare the same meaning as
         * our known types, see above)
         */
        return types
            .stream()
            .filter(type -> 
                type.getName().equals("dateCreated")
                || type.getName().equals("createdAt")
                || type.getName().equals("creationDate"))
            .map(type -> pidRecord.getPropertyValues(type.getIdentifier()))
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
    }

    /**
     * Tries to get the date when a FAIR DO was modified from a PID record.
     * 
     * Strategy:
     * - try to get it from known "dateModified" types
     * - as a fallback, try to get it by its human readable name
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the date, if it could been extracted.
     * @throws IOException on IO errors regarding resolving types.
     */
    public Optional<Date> findDateModified(PIDRecord pidRecord) throws IOException {
        /* try known types */
        List<String> knownDateTypes = Arrays.asList(Operations.KNOWN_DATE_MODIFIED);
        Optional<Date> date = knownDateTypes
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
        if (date.isPresent()) {
            return date;
        }

        /* TODO try to find types extending or relating otherwise to known types
         *      (currently not supported by our TypeDefinition) */
        Collection<TypeDefinition> types = new ArrayList<>();
        List<CompletableFuture<?>> futures = Streams
                .stream(pidRecord.getPropertyIdentifiers().stream())
                .filter(attributePid -> this.typingService.isIdentifierRegistered(attributePid))
                .map(attributePid -> {
                    return this.typingService
                            .describeType(attributePid)
                            .thenAcceptAsync(types::add);
                })
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        /*
         * as a last fallback, try find types with human readable names containing
         * "dateModified" or "lastModified" or "modificationDate".
         * 
         * This can be removed as soon as we have some default FAIR DO types new type
         * definitions can refer to (e.g. "extend" them or declare the same meaning as
         * our known types, see above)
         */
        return types
            .stream()
            .filter(type -> 
                type.getName().equals("dateModified")
                || type.getName().equals("lastModified")
                || type.getName().equals("modificationDate"))
            .map(type -> pidRecord.getPropertyValues(type.getIdentifier()))
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
    }

    /**
     * Tries to extract a Date object from a String.
     * 
     * @param dateString the date string to extract the date from.
     * @return the extracted Date object.
     */
    protected Optional<Date> extractDate(String dateString) {
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
        try {
            DateTime dateTime = dateFormatter.parseDateTime(dateString);
            return Optional.of(dateTime.toDate());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
