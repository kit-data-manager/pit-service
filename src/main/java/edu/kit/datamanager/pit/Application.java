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

import edu.kit.datamanager.pit.cli.CliTaskBootstrap;
import edu.kit.datamanager.pit.cli.CliTaskWriteFile;
import edu.kit.datamanager.pit.cli.ICliTask;
import edu.kit.datamanager.pit.cli.PidSource;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.impl.TypingService;
import edu.kit.datamanager.pit.resolver.Resolver;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import edu.kit.datamanager.pit.typeregistry.impl.TypeApi;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaSetGenerator;
import edu.kit.datamanager.pit.web.converter.SimplePidRecordConverter;
import edu.kit.datamanager.security.filter.KeycloakJwtProperties;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    protected static final String CMD_BOOTSTRAP = "bootstrap";
    protected static final String CMD_WRITE_FILE = "write-file";

    protected static final String SOURCE_FROM_PREFIX = "all-pids-from-prefix";
    protected static final String SOURCE_KNOWN_PIDS = "known-pids";
    
    protected static final String ERROR_COMMUNICATION = "Communication error: {}";
    protected static final String ERROR_CONFIGURATION = "Configuration error: {}";

    /**
     * This is a threshold considered very long for a http request.
     * Usually used in logging context
     */
    public static final long LONG_HTTP_REQUEST_THRESHOLD = 400;

    @Bean
    @Scope("prototype")
    public Logger logger(InjectionPoint injectionPoint) {
        Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
        return LoggerFactory.getLogger(targetClass.getCanonicalName());
    }

    public static ExecutorService newExecutor() {
        return Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());
    }

    @Bean
    public SchemaSetGenerator schemaSetGenerator(ApplicationProperties props) {
        return new SchemaSetGenerator(props);
    }

    @Bean
    public ITypeRegistry typeRegistry(ApplicationProperties props, SchemaSetGenerator schemaSetGenerator) {
        return new TypeApi(props, schemaSetGenerator);
    }

    @Bean
    public Resolver resolver(ITypingService identifierSystem) {
        return new Resolver(identifierSystem);
    }

    @Bean
    public ITypingService typingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry) {
        return new TypingService(identifierSystem, typeRegistry);
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

        final boolean cliArgsAmountValid = args != null && args.length != 0 && args.length >= 2;
        
        if (cliArgsAmountValid) {
            ICliTask task = null;
            Stream<String> pidSource = null;
            
            if (Objects.equals(args[1], SOURCE_FROM_PREFIX)) {
                try {
                    pidSource = PidSource.fromPrefix(context);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOG.error(ERROR_COMMUNICATION, e.getMessage());
                    exitApp(context, 1);
                }
            } else if (Objects.equals(args[1], SOURCE_KNOWN_PIDS)) {
                pidSource = PidSource.fromKnown(context);
            }

            if (Objects.equals(args[0], CMD_BOOTSTRAP)) {
                task = new CliTaskBootstrap(context, pidSource);
            } else if (Objects.equals(args[0], CMD_WRITE_FILE)) {
                task = new CliTaskWriteFile(pidSource);
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
                LOG.error(ERROR_CONFIGURATION, e.getMessage());
                exitApp(context, 1);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error(ERROR_COMMUNICATION, e.getMessage());
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
        LOG.error("java -jar TypedPIDMaker.jar bootstrap known-pids");
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
