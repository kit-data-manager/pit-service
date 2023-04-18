package edu.kit.datamanager.pit.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
        // we need to resolve types without streams to forward possible exceptions
        Collection<TypeDefinition> types = new ArrayList<>();
        for (String attributePid : pidRecord.getPropertyIdentifiers()) {
            if (this.typingService.isIdentifierRegistered(attributePid)) {
                TypeDefinition type = this.typingService.describeType(attributePid);
                types.add(type);
            }
        }

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
        // we need to resolve types without streams to forward possible exceptions
        Collection<TypeDefinition> types = new ArrayList<>();
        for (String attributePid : pidRecord.getPropertyIdentifiers()) {
            if (this.typingService.isIdentifierRegistered(attributePid)) {
                TypeDefinition type = this.typingService.describeType(attributePid);
                types.add(type);
            }
        }

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
