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

package edu.kit.datamanager.pit.pidgeneration;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * A thin wrapper around a suffix string.
 * <p>
 * The purpose is to indicate that this string is missing the prefix part and is
 * not used as a PID accidentially.
 */
public class PidSuffix {
    private final String suffix;

    public PidSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Ensures a string is prefixed with the given prefix.
     * <p>
     * It makes sure the prefix is not added, if the string already starts with the
     * prefix.
     *
     * @param maybeSuffix the string to prefix.
     * @param prefix      the prefix to add.
     * @return the string with the prefix added.
     */
    @WithSpan(kind = SpanKind.INTERNAL)
    public static String asPrefixedChecked(String maybeSuffix, String prefix) {
        if (!maybeSuffix.startsWith(prefix)) {
            return prefix + maybeSuffix;
        } else {
            return maybeSuffix;
        }
    }

    /**
     * Returns the suffix string.
     *
     * @return the suffix without any prefix.
     */
    @WithSpan(kind = SpanKind.INTERNAL)
    public String get() {
        return suffix;
    }

    /**
     * Returns the suffix string with the given prefix prepended.
     *
     * @param prefix the prefix to prepend.
     * @return the prefix + suffix.
     */
    @WithSpan(kind = SpanKind.INTERNAL)
    public String getWithPrefix(String prefix) {
        return prefix + suffix;
    }
}
