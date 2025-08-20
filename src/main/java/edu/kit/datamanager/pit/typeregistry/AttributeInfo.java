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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.configuration.PIISpanAttribute;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * @param pid        the pid of this attribute
 * @param name       a human-readable name, defined in the DTR for this type.
 *                   Note this is usually different from the name in a specific profile!
 * @param typeName   name of the schema type of this attribute in the DTR,
 *                   e.g. "Profile", "InfoType", "Special-Info-Type", ...
 * @param jsonSchema the json schema to validate a value of this attribute
 */
public record AttributeInfo(
        String pid,
        String name,
        String typeName,
        Collection<SchemaInfo> jsonSchema
) {
    private static final Logger log = LoggerFactory.getLogger(AttributeInfo.class);

    @WithSpan(kind = SpanKind.INTERNAL)
    @Counted(value = "validation_attribute", description = "Total number of attribute validations")
    @Timed(value = "validation_attribute_time", description = "Time taken for attribute validation")
    public boolean validate(@PIISpanAttribute String value) {
        return this.jsonSchema().stream()
                .filter(schemaInfo -> schemaInfo.error() == null)
                .filter(schemaInfo -> schemaInfo.schema() != null)
                .peek(schemaInfo -> log.warn("Found valid schema from {} to validate {} / {}.", schemaInfo.origin(), pid, value))
                .anyMatch(schemaInfo -> this.validate(schemaInfo.schema(), value));
    }

    private boolean validate(JsonSchema schema, String value) {
        try {
            JsonNode toValidate = valueToJsonNode(value);
            Set<ValidationMessage> errors = schema.validate(toValidate, executionContext -> {
                // By default, since Draft 2019-09, the format keyword only generates annotations and not assertions
                executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
            });
            if (!errors.isEmpty()) {
                log.warn("Validation errors for value '{}': {}", value, errors);
            }
            return errors.isEmpty();
        } catch (Exception e) {
            log.error("Exception during validation for value '{}': {}", value, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Converts the given value to a JsonNode.
     *
     * @param value the value to convert
     * @return a JsonNode representation of the value
     */
    public static JsonNode valueToJsonNode(String value) {
        JsonNode toValidate;
        if (value.isBlank()) {
            return new TextNode(value);
        }
        try {
            toValidate = Application.jsonObjectMapper().readTree(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse value '{}' as JSON, treating it as a plain text node.", value);
            toValidate = new TextNode(value);
        }
        return toValidate;
    }
}