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

import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.impl.local.PidDatabaseObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> supportedTypes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> supportedLocations = new ArrayList<>();

    @Field(type = FieldType.Text)
    private List<String> read = new ArrayList<>();

    protected PidRecordElasticWrapper() {
        // for PidRecordElasticRepository
    }

    public PidRecordElasticWrapper(PIDRecord pidRecord, Operations recordOperations) {
        pid = pidRecord.getPid();
        PidDatabaseObject simple = new PidDatabaseObject(pidRecord);
        this.attributes = simple.getEntries();
        this.read.add("anonymousUser");

        this.supportedTypes = recordOperations.findSupportedTypes(pidRecord);
        this.supportedLocations = recordOperations.findSupportedLocations(pidRecord);
        
        try {
            this.created = recordOperations.findDateCreated(pidRecord).orElse(null);
            this.lastUpdate = recordOperations.findDateModified(pidRecord).orElse(null);
        } catch (IOException e) {
            LOG.error("Could not retrieve date from record (pid: " + pidRecord.getPid() + ").", e);
            e.printStackTrace();
        }
    }

    /**
     * Generated with Lombok
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((lastUpdate == null) ? 0 : lastUpdate.hashCode());
        result = prime * result + ((supportedTypes == null) ? 0 : supportedTypes.hashCode());
        result = prime * result + ((supportedLocations == null) ? 0 : supportedLocations.hashCode());
        result = prime * result + ((read == null) ? 0 : read.hashCode());
        return result;
    }

    /**
     * Generated with Lombok
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PidRecordElasticWrapper other = (PidRecordElasticWrapper) obj;
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (lastUpdate == null) {
            if (other.lastUpdate != null)
                return false;
        } else if (!lastUpdate.equals(other.lastUpdate))
            return false;
        if (supportedTypes == null) {
            if (other.supportedTypes != null)
                return false;
        } else if (!supportedTypes.equals(other.supportedTypes))
            return false;
        if (supportedLocations == null) {
            if (other.supportedLocations != null)
                return false;
        } else if (!supportedLocations.equals(other.supportedLocations))
            return false;
        if (read == null) {
            if (other.read != null)
                return false;
        } else if (!read.equals(other.read))
            return false;
        return true;
    }
}
