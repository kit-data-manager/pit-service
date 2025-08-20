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

package edu.kit.datamanager.pit.typeregistry;

import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.configuration.PIISpanAttribute;
import edu.kit.datamanager.pit.domain.ImmutableList;
import edu.kit.datamanager.pit.domain.PIDRecord;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RegisteredProfile(
        String pid,
        boolean allowAdditionalAttributes,
        ImmutableList<RegisteredProfileAttribute> attributes
) {
    @WithSpan(kind = SpanKind.INTERNAL)
    @Timed(value = "validation_registered_profile", description = "Time taken for validation of a PID record against a registered profile")
    public void validateAttributes(
            @PIISpanAttribute PIDRecord pidRecord,
            @SpanAttribute boolean alwaysAllowAdditionalAttributes
    ) throws RecordValidationException {
        Span.current()
                .setAttribute("profile.pid", this.pid)
                .setAttribute("profile.allowAdditionalAttributes", this.allowAdditionalAttributes)
                .setAttribute("profile.alwaysAllowAdditionalAttributes", alwaysAllowAdditionalAttributes);

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