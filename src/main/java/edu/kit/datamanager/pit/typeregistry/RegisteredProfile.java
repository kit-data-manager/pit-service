package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.ImmutableList;
import edu.kit.datamanager.pit.domain.PidRecord;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RegisteredProfile(
        String pid,
        boolean allowAdditionalAttributes,
        ImmutableList<RegisteredProfileAttribute> attributes
) {
    public void validateAttributes(
            PidRecord pidRecord,
            boolean alwaysAllowAdditionalAttributes
    ) throws RecordValidationException
    {
        Set<String> attributesNotDefinedInProfile = pidRecord.getPropertyIdentifiers().stream()
                .filter(recordKey -> attributes.items().stream().noneMatch(
                        profileAttribute -> Objects.equals(profileAttribute.pid(), recordKey)))
                .collect(Collectors.toSet());


        boolean additionalAttributesForbidden = !this.allowAdditionalAttributes && !alwaysAllowAdditionalAttributes;
        boolean violatesAdditionalAttributes = additionalAttributesForbidden && !attributesNotDefinedInProfile.isEmpty();
        if (violatesAdditionalAttributes) {
            throw new RecordValidationException(
                    pidRecord,
                    String.format("Attributes %s are not allowed in profile %s",
                            String.join(", ", attributesNotDefinedInProfile),
                            this.pid)
            );
        }

        for (RegisteredProfileAttribute profileAttribute : this.attributes.items()) {
            if (profileAttribute.violatesMandatoryProperty(pidRecord)) {
                throw new RecordValidationException(
                        pidRecord,
                        String.format("Attribute %s missing, but is mandatory in profile %s",
                                profileAttribute.pid(),
                                this.pid)
                );
            }
            if (profileAttribute.violatesRepeatableProperty(pidRecord)) {
                throw new RecordValidationException(
                        pidRecord,
                        String.format("Attribute %s is not repeatable in profile %s, but has multiple values",
                                profileAttribute.pid(),
                                this.pid)
                );
            }
        }
    }
}