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

import edu.kit.datamanager.pit.domain.PIDRecord;
import io.micrometer.core.annotation.Counted;

public record RegisteredProfileAttribute(
        String pid,
        boolean mandatory,
        boolean repeatable
) {
    @Counted(value = "registered_profile_attribute_violations", description = "Count of violations of registered profile attributes")
    public boolean violatesMandatoryProperty(PIDRecord pidRecord) {
        boolean contains = pidRecord.getPropertyIdentifiers().contains(this.pid)
                && pidRecord.getPropertyValues(this.pid).length > 0;
        return this.mandatory && !contains;
    }

    @Counted(value = "registered_profile_attribute_repeatable_violations", description = "Count of violations of repeatable properties in registered profile attributes")
    public boolean violatesRepeatableProperty(PIDRecord pidRecord) {
        boolean repeats = pidRecord.getPropertyValues(this.pid).length > 1;
        return !this.repeatable && repeats;
    }
}
