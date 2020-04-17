/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.pit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pidsystem.impl.FakeIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.impl.TypingService;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.typeregistry.impl.TypeRegistry;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.xml.validation.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xml.sax.SAXException;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan({"edu.kit.datamanager", "edu.kit.datamanager.messaging.client"})
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

//  @Autowired
//  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;
    @Bean
    @Scope("prototype")
    public Logger logger(InjectionPoint injectionPoint) {
        Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
        return LoggerFactory.getLogger(targetClass.getCanonicalName());
    }

    @Bean
    public ITypeRegistry typeRegistry() {
        return new TypeRegistry();
    }

    @Bean
    public IIdentifierSystem identifierSystem() {
        return new FakeIdentifierSystem();// new HandleSystemRESTAdapter(applicationProperties());
    }

    @Bean
    public ITypingService typingService() throws IOException {
        return new TypingService(identifierSystem(), typeRegistry(), typeCache());
    }

    @Bean(name = "OBJECT_MAPPER_BEAN")
    public ObjectMapper jsonObjectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
                .modules(new JavaTimeModule())
                .build();
    }

    @Bean
    public LoadingCache<String, TypeDefinition> typeCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .removalListener((RemovalNotification<String, TypeDefinition> rn) -> {
                    LOG.trace("Removing type definition located at {} from schema cache. Cause: {}", rn.getKey(), rn.getCause());
                })
                .build(new CacheLoader<String, TypeDefinition>() {
                    @Override
                    public TypeDefinition load(String typeIdentifier) throws IOException, URISyntaxException {
                        LOG.trace("Loading type definition for identifier {} to cache.", typeIdentifier);
                        return typeRegistry().queryTypeDefinition(typeIdentifier);
                    }
                });
    }

//  @Bean
//  public WebMvcConfigurer corsConfigurer(){
//    return new WebMvcConfigurer(){
//      @Override
//      public void addCorsMappings(CorsRegistry registry){
//        registry.addMapping("/**").allowedOrigins("http://localhost:8090").exposedHeaders("Content-Length").allowedHeaders("Accept");
//      }
//    };
//  }
//  @Bean
//  @Primary
//  public RequestMappingHandlerAdapter adapter(){
//    return requestMappingHandlerAdapter;
//  }
//  @Bean
//  public JsonViewSupportFactoryBean views(){
//    return new JsonViewSupportFactoryBean();
//  }
//  @Bean
//  @ConfigurationProperties("repo")
//  public ApplicationProperties applicationProperties(){
//    return new ApplicationProperties();
//  }
//  @Bean
//  public IMessagingService messagingService(){
//    return new RabbitMQMessagingService();
//  }
    @Bean
    @ConfigurationProperties("pit")
    public ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        System.out.println("Spring is running!");
    }

}
