package edu.kit.datamanager.pit.configuration;

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

import edu.kit.datamanager.configuration.SearchConfiguration;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.http.HttpHeaders;

/**
 *
 * @author jejkal
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "edu.kit.datamanager.repo")
@ComponentScan(basePackages = {"edu.kit.datamanager"})
@ConditionalOnProperty(prefix = "repo.search", name = "enabled", havingValue = "true")
public class ElasticConfiguration {

    @Autowired
    private SearchConfiguration searchConfiguration;

    @Bean
    public RestHighLevelClient client() {
        //required for compatibility to Elastic 8.X ... might not work and should be removed with spring-boot 3.X
        HttpHeaders compatibilityHeaders = new HttpHeaders();
        compatibilityHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
        compatibilityHeaders.add("Content-Type", "application/vnd.elasticsearch+json;compatible-with=7");

        String indexUrl = searchConfiguration.getUrl().toString();
        String hostnamePort = indexUrl.substring(indexUrl.indexOf("//") + 2);

        ClientConfiguration clientConfiguration
                = ClientConfiguration.builder()
                        .connectedTo(hostnamePort)
                        .withDefaultHeaders(compatibilityHeaders)
                        .build();

        return RestClients.create(clientConfiguration).rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }

}
