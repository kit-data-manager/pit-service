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

package edu.kit.datamanager.pit.web;

import edu.kit.datamanager.pit.domain.PIDRecord;

import java.util.List;
import java.util.Map;

/**
 * Response object for batch record operations.
 * Contains a list of the processed PID records and a mapping of the user-provided "fictionary" identifiers to their corresponding real record Handle PIDs.
 * This mapping was used to link the user-provided identifiers with the actual records in the system.
 * <p>
 *
 * @param pidRecords List of PIDRecord objects representing the processed records. (List<PIDRecord>)
 * @param mapping    Map where keys are user-provided identifiers (fictionary) and values are the corresponding real record Handle PIDs. (Map<String, String>)
 * @see PIDRecord
 */
public record BatchRecordResponse(List<PIDRecord> pidRecords, Map<String, String> mapping) {
}
