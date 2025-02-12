package edu.kit.datamanager.pit.domain;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import org.apache.commons.lang3.stream.Streams;
import java.time.ZonedDateTime;

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

    private ITypeRegistry typeRegistry;
    private IIdentifierSystem identifierSystem;

    public Operations(ITypeRegistry typeRegistry, IIdentifierSystem identifierSystem) {
        this.typeRegistry = typeRegistry;
        this.identifierSystem = identifierSystem;
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

        Collection<AttributeInfo> types = new ArrayList<>();
        List<CompletableFuture<?>> futures = Streams.failableStream(
                pidRecord.getPropertyIdentifiers().stream())
                .filter(attributePid -> this.identifierSystem.isPidRegistered(attributePid))
                .map(attributePid -> {
                    return this.typeRegistry
                            .queryAttributeInfo(attributePid)
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
                type.name().equalsIgnoreCase("dateCreated")
                || type.name().equalsIgnoreCase("createdAt")
                || type.name().equalsIgnoreCase("creationDate"))
            .map(type -> pidRecord.getPropertyValues(type.pid()))
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

        Collection<AttributeInfo> types = new ArrayList<>();
        List<CompletableFuture<?>> futures = Streams.failableStream(pidRecord.getPropertyIdentifiers().stream())
                .filter(attributePid -> this.identifierSystem.isPidRegistered(attributePid))
                .map(attributePid -> {
                    return this.typeRegistry
                            .queryAttributeInfo(attributePid)
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
                type.name().equalsIgnoreCase("dateModified")
                || type.name().equalsIgnoreCase("lastModified")
                || type.name().equalsIgnoreCase("modificationDate"))
            .map(type -> pidRecord.getPropertyValues(type.pid()))
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
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(dateString, dateFormatter);
            return Optional.of(Date.from(dateTime.toInstant()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
