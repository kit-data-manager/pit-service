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

package edu.kit.datamanager.pit.pidgeneration.generators;

import edu.kit.datamanager.pit.pidgeneration.PidSuffix;
import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Generates a PID suffix based on a contained generator and returns the result
 * in lower case.
 */
@Observed
public class PidSuffixGenLowerCase implements PidSuffixGenerator {

    private final PidSuffixGenerator generator;

    public PidSuffixGenLowerCase(PidSuffixGenerator generator) {
        this.generator = generator;
    }

    @Override
    @WithSpan(kind = SpanKind.INTERNAL)
    public PidSuffix generate() {
        return new PidSuffix(this.generator.generate().get().toLowerCase());
    }

}
