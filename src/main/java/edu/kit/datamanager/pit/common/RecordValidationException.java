/*
 * Copyright (c) 2024 Karlsruhe Institute of Technology.
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

package edu.kit.datamanager.pit.common;

import edu.kit.datamanager.pit.domain.PIDRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Indicates that a PID was given which could not be resolved to answer the
 * request properly.
 */
public class RecordValidationException extends ResponseStatusException {

    private static final String VALIDATION_OF_RECORD = "Validation of record ";
    private static final long serialVersionUID = 1L;
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

    // For cases in which the PID record should be appended to the error response.
    private final transient PIDRecord pidRecord;

    public RecordValidationException(PIDRecord pidRecord) {
        super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed.");
        this.pidRecord = pidRecord;
    }

    public RecordValidationException(PIDRecord pidRecord, String reason) {
      super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed. Reason: " + reason);
      this.pidRecord = pidRecord;
    }

    public RecordValidationException(PIDRecord pidRecord, String reason, Exception e) {
      super(HTTP_STATUS, VALIDATION_OF_RECORD + pidRecord.getPid() + " failed. Reason: " + reason, e);
      this.pidRecord = pidRecord;
    }

    public PIDRecord getPidRecord() {
        return pidRecord;
    }
}
