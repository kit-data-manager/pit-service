/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.pit.elasticsearch;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.local.PidDatabaseObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "typedpidmaker")
public class PidRecordElasticWrapper {

    @Id
    private String pid;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, ArrayList<String>> attributes = new HashMap<>();

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date created;

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date lastUpdate;

    /** For hibernate */
    public PidRecordElasticWrapper() {}

    public PidRecordElasticWrapper(PIDRecord pidRecord) {
        pid = pidRecord.getPid();
        PidDatabaseObject simple = new PidDatabaseObject(pidRecord);
        this.attributes = simple.getEntries();

        // TODO set dates
        //pidRecord.getDates().stream().filter(d -> (DATE_TYPE.CREATED.equals(d.getType()))).forEachOrdered(d -> {
        //    created = Date.from(d.getValue());
        //});
        //lastUpdate = Date.from(Instant.now());
    }
}
