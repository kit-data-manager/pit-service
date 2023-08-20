/*
 * Copyright 2022 Karlsruhe Institute of Technology.
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

import java.util.Collection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Elastic repository for indexing PID records.
 */
@ConditionalOnProperty(prefix = "repo.search", name = "enabled", havingValue = "true")
public interface PidRecordElasticRepository extends ElasticsearchRepository<PidRecordElasticWrapper, String> {

    Page<PidRecordElasticWrapper> findByPid(String pid, Pageable pageable);

    @Query("{\"match\": {\"supportedLocations\": \"?0\"}}")
    Collection<PidRecordElasticWrapper> findBySupportedLocationsContain(
        String location
    );

    @Query("{\"match\": {\"supportedTypes\": \"?0\"}}")
    Collection<PidRecordElasticWrapper> findBySupportedTypesContain(String type);

}
