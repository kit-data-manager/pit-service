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

import edu.kit.datamanager.pit.cli.CliTaskBootstrap;
import edu.kit.datamanager.pit.cli.CliTaskWriteFile;
import edu.kit.datamanager.pit.cli.ICliTask;
import edu.kit.datamanager.pit.cli.PidSource;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.impl.TypingService;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.typeregistry.impl.TypeRegistry;
import edu.kit.datamanager.pit.web.converter.SimplePidRecordConverter;
import edu.kit.datamanager.security.filter.KeycloakJwtProperties;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@EnableScheduling
@EntityScan({ "edu.kit.datamanager" })
// Required for "DAO" objects to work, needed for messaging service and database
// mappings
@EnableJpaRepositories("edu.kit.datamanager")
// Detects services and components in datamanager dependencies (service-base and
// repo-core)
@ComponentScan({ "edu.kit.datamanager" })
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

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
    public ITypingService typingService(IIdentifierSystem identifierSystem, ApplicationProperties props) throws IOException {
        return new TypingService(identifierSystem, typeRegistry(), typeCache(props));
    }

    @Bean(name = "OBJECT_MAPPER_BEAN")
    public static ObjectMapper jsonObjectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISODate
                .modules(new JavaTimeModule())
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // BufferingClientHttpRequestFactory allows us to read the response more than
        // once - Necessary for debugging.
        restTemplate.setRequestFactory(
                new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient())));
        return restTemplate;
    }

    @Bean
    public HttpClient httpClient() {
        return CachingHttpClientBuilder
                .create()
                .setCacheConfig(cacheConfig())
                .build();
    }

    @Bean
    public CacheConfig cacheConfig() {
        return CacheConfig 
                .custom()
                .setMaxObjectSize(500000) // 500KB
                .setMaxCacheEntries(2000)
                // Set this to false and a response with queryString
                // will be cached when it is explicitly cacheable
                // .setNeverCacheHTTP10ResponsesWithQueryString(false)
                .build();
    }

    @Bean
    public LoadingCache<String, TypeDefinition> typeCache(ApplicationProperties props){
        int maximumsize = props.getMaximumSize();
        long expireafterwrite = props.getExpireAfterWrite();
        return CacheBuilder.newBuilder()
                .maximumSize(maximumsize)
                .expireAfterWrite(expireafterwrite, TimeUnit.MINUTES)
                .removalListener((RemovalNotification<String, TypeDefinition> rn) -> LOG.trace(
                        "Removing type definition located at {} from schema cache. Cause: {}", rn.getKey(),
                        rn.getCause()))
                .build(new CacheLoader<String, TypeDefinition>() {
                    @Override
                    public TypeDefinition load(String typeIdentifier) throws IOException, URISyntaxException {
                        LOG.trace("Loading type definition for identifier {} to cache.", typeIdentifier);
                        return typeRegistry().queryTypeDefinition(typeIdentifier);
                    }
                });
    }

    @ConfigurationProperties("pit")
    public ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }

    @Bean
    // Reads keycloak related settings from properties.application.
    public KeycloakJwtProperties properties() {
      return new KeycloakJwtProperties();
    }

    @Bean
    public HttpMessageConverter<PIDRecord> simplePidRecordConverter() {
        return new SimplePidRecordConverter();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        System.out.println("Spring is running!");

        final boolean cliArgsGiven = args != null && args.length != 0;
        final boolean cliArgsAmountValid = cliArgsGiven && args.length >= 2;
        
        final String writeFileCmd = "write-file";
        final String bootstrapCmd = "bootstrap";
        
        final String sourceFromPrefix = "all-pids-from-prefix";
        final String sourceKnownPids = "known-pids";

        final String errorCommunication = "Communication error: {}";
        final String errorConfiguration = "Configuration error: {}";

        if (cliArgsAmountValid) {
            ICliTask task = null;
            Stream<String> pidSource = null;
            
            if (Objects.equals(args[1], sourceFromPrefix)) {
                try {
                    pidSource = PidSource.fromPrefix(context);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOG.error(errorCommunication, e.getMessage());
                    exitApp(context, 1);
                }
            } else if (Objects.equals(args[1], sourceKnownPids)) {
                pidSource = PidSource.fromKnown(context);
            }

            if (Objects.equals(args[0], bootstrapCmd)) {
                task = new CliTaskBootstrap(context, pidSource);
            } else if (Objects.equals(args[0], writeFileCmd)) {
                task = new CliTaskWriteFile(context, pidSource);
            }

            try {
                if (task != null && pidSource != null) {
                    // ---process task---
                    if (task.process()) {
                        exitApp(context, 0);
                    }
                } else {
                    printUsage(args);
                    exitApp(context, 1);
                }
            } catch (InvalidConfigException e) {
                e.printStackTrace();
                LOG.error(errorConfiguration, e.getMessage());
                exitApp(context, 1);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error(errorCommunication, e.getMessage());
                exitApp(context, 1);
            }
        }
    }

    private static void printUsage(String[] args) {
        String firstArg = args[0].replaceAll("[\r\n]","");
        String secondArg = args[1].replaceAll("[\r\n]","");
        LOG.error("Got commands: {} and {}", firstArg, secondArg);
        LOG.error("CLI usage incorrect. Usage:");
        LOG.error("java -jar TypedPIDMaker.jar [ACTION] [SOURCE]");
        LOG.error("java -jar TypedPIDMaker.jar bootstrap all-pids-from-prefix");
        LOG.error("java -jar TypedPIDMaker.jar write-file all-pids-from-prefix");
        LOG.error("java -jar TypedPIDMaker.jar write-file known-pids");
    }

    private static void exitApp(ConfigurableApplicationContext context, int errCode) {
        context.close();
        try {
            Thread.sleep(2 * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (errCode != 0) {
            LOG.error("Exited with error.");
        } else {
            LOG.info("Success");
        }
        System.exit(errCode);
    }

}
