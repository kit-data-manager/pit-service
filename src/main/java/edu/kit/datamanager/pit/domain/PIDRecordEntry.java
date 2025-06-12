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

package edu.kit.datamanager.pit.domain;

import lombok.Data;

@Data
public class PIDRecordEntry implements Cloneable {
    private String key;
    private String name;
    private String value;

    @Override
    public PIDRecordEntry clone() {
        try {
            PIDRecordEntry clone = (PIDRecordEntry) super.clone();
            clone.setKey(this.key);
            clone.setName(this.name);
            clone.setValue(this.value);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
