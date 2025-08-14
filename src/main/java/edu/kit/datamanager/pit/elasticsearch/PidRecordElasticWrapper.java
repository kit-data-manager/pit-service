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
 */
package edu.kit.datamanager.pit.elasticsearch;

import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.local.PidDatabaseObject;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.IOException;
import java.util.*;

@Observed
@Document(indexName = "typedpidmaker")
public class PidRecordElasticWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(PidRecordElasticWrapper.class);

    @Id
    private String pid;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, ArrayList<String>> attributes = new HashMap<>();

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date created;

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date lastUpdate;

    @Field(type = FieldType.Text)
    private final List<String> read = new ArrayList<>();

    @WithSpan(kind = SpanKind.INTERNAL)
    public PidRecordElasticWrapper(PIDRecord pidRecord, Operations dateOperations) {
        pid = pidRecord.getPid();
        PidDatabaseObject simple = new PidDatabaseObject(pidRecord);
        this.attributes = simple.getEntries();
        this.read.add("anonymousUser");

        try {
            this.created = dateOperations.findDateCreated(pidRecord).orElse(null);
            this.lastUpdate = dateOperations.findDateModified(pidRecord).orElse(null);
        } catch (IOException e) {
            LOG.error("Could not retrieve date from record (pid: " + pidRecord.getPid() + ").", e);
            e.printStackTrace();
        }
    }
}
