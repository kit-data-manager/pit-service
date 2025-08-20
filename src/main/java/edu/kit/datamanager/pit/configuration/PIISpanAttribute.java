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
package edu.kit.datamanager.pit.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark method parameters that contain PII (Personally Identifiable Information)
 * for conditional inclusion in OpenTelemetry spans.
 * <p>
 * This annotation works like @SpanAttribute but only processes the parameter
 * when pit.observability.includePiiInTraces=true is configured.
 * <p>
 * Usage:
 * <pre>
 * public void myMethod(@PIISpanAttribute("pid") String pidValue,
 *                      @PIISpanAttribute PIDRecord record) {
 *     // method implementation
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PIISpanAttribute {

    /**
     * The name of the span attribute. If not provided, the parameter name will be used.
     *
     * @return the span attribute name
     */
    String value() default "";
}
