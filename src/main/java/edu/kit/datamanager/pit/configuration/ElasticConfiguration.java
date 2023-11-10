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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/*
 * https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/data.html#data.nosql.elasticsearch.connecting-using-rest.javaapiclient
 * https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/#elasticsearch.clients.restclient
 */
@Configuration
@ConditionalOnProperty(prefix = "repo.search", name="enabled", havingValue = "true", matchIfMissing = false)
public class ElasticConfiguration extends ElasticsearchConfiguration {

    private final SearchConfiguration searchConfiguration;
    
    public ElasticConfiguration(@Autowired SearchConfiguration searchConfiguration) {
        this.searchConfiguration = searchConfiguration;
    }

    @Override
	public ClientConfiguration clientConfiguration() {
        String serverUrl = searchConfiguration.getUrl().toString();
        serverUrl = serverUrl.replace("http://", "");
        serverUrl = serverUrl.replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(serverUrl)
                .build();
    }
}
