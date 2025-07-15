
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

package edu.kit.datamanager.pit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class ConnectedPIDsTest {
    private static final int RECORD_COUNT = 16;
    private static final int LARGE_RECORD_COUNT = 200;

    // Valid DTR keys for testing connections
    private static final String[] VALID_CONNECTION_KEYS = {
            "21.T11148/432132bdbd946b2baf2b",
            "21.T11148/ab53242825e85a0a7f76",
            "21.T11148/2a1cad55473b20407c78"
    };

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    private ObjectMapper mapper;
    @Autowired
    private KnownPidsDao knownPidsDao;

    @BeforeEach
    void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        this.mapper = new ObjectMapper();
        knownPidsDao.deleteAll();
    }

    @Test
    void checkTestSetup() {
        assertNotNull(mockMvc, "MockMvc should be initialized");
        assertNotNull(mapper, "Object mapper should be initialized");
        assertNotNull(knownPidsDao, "KnownPidsDao should be initialized");
        assertEquals(0, knownPidsDao.count(), "Database should be empty at test start");
    }

    @Test
    @DisplayName("Test comprehensive PIDBuilder functionality")
    void testPIDBuilderAllMethods() {
        Long testSeed = 12345L;

        // Test default constructor
        PIDBuilder defaultBuilder = new PIDBuilder();
        assertNotNull(defaultBuilder, "Default PIDBuilder constructor should create non-null instance");
        assertNotNull(defaultBuilder.build(), "Default PIDBuilder should build non-null PID");

        // Test constructor with seed
        PIDBuilder seededBuilder = new PIDBuilder(testSeed);
        assertNotNull(seededBuilder, "Seeded PIDBuilder constructor should create non-null instance");
        assertEquals(testSeed, seededBuilder.seed, "Seeded PIDBuilder should have the correct seed value");

        // Test withSeed method
        PIDBuilder seedModified = new PIDBuilder().withSeed(testSeed);
        assertEquals(testSeed, seedModified.seed, "withSeed method should set the correct seed value");

        // Test all prefix methods
        PIDBuilder validPrefixBuilder = new PIDBuilder(testSeed).validPrefix();
        String validPid = validPrefixBuilder.build();
        assertTrue(validPid.startsWith("sandboxed/"), "validPrefix should create PID starting with 'sandboxed/'");

        PIDBuilder unauthorizedPrefixBuilder = new PIDBuilder(testSeed).unauthorizedPrefix();
        String unauthorizedPid = unauthorizedPrefixBuilder.build();
        assertTrue(unauthorizedPid.startsWith("0.NA/"), "unauthorizedPrefix should create PID starting with '0.NA/'");

        PIDBuilder emptyPrefixBuilder = new PIDBuilder(testSeed).emptyPrefix();
        String emptyPrefixPid = emptyPrefixBuilder.build();
        assertTrue(emptyPrefixPid.startsWith("/"), "emptyPrefix should create PID starting with '/'");

        PIDBuilder invalidPrefixBuilder = new PIDBuilder(testSeed).invalidCharactersPrefix();
        String invalidPrefixPid = invalidPrefixBuilder.build();
        assertNotNull(invalidPrefixPid, "invalidCharactersPrefix should create non-null PID");

        // Test withPrefix method
        PIDBuilder customPrefixBuilder = new PIDBuilder(testSeed).withPrefix("custom.prefix");
        String customPrefixPid = customPrefixBuilder.build();
        assertTrue(customPrefixPid.startsWith("custom.prefix/"), "withPrefix should create PID starting with the specified prefix");

        // Test all suffix methods
        PIDBuilder validSuffixBuilder = new PIDBuilder(testSeed).validPrefix().validSuffix();
        String validSuffixPid = validSuffixBuilder.build();
        assertNotNull(validSuffixPid, "validSuffix should create non-null PID");

        PIDBuilder emptySuffixBuilder = new PIDBuilder(testSeed).validPrefix().emptySuffix();
        String emptySuffixPid = emptySuffixBuilder.build();
        assertTrue(emptySuffixPid.endsWith("/"), "emptySuffix should create PID ending with '/'");

        PIDBuilder invalidSuffixBuilder = new PIDBuilder(testSeed).validPrefix().invalidCharactersSuffix();
        String invalidSuffixPid = invalidSuffixBuilder.build();
        assertNotNull(invalidSuffixPid, "invalidCharactersSuffix should create non-null PID");

        // Test withSuffix method
        PIDBuilder customSuffixBuilder = new PIDBuilder(testSeed).validPrefix().withSuffix("custom-suffix");
        String customSuffixPid = customSuffixBuilder.build();
        assertTrue(customSuffixPid.endsWith("custom-suffix"), "withSuffix should create PID ending with the specified suffix");

        // Test clone method
        PIDBuilder originalBuilder = new PIDBuilder(testSeed).validPrefix().validSuffix();
        PIDBuilder clonedBuilder = originalBuilder.clone();
        assertEquals(originalBuilder.build(), clonedBuilder.build(), "Cloned builder should produce the same PID");
        assertNotSame(originalBuilder, clonedBuilder, "Cloned builder should be a different instance");

        // Test clone(PIDBuilder) method
        PIDBuilder targetBuilder = new PIDBuilder();
        targetBuilder.clone(originalBuilder);
        assertEquals(originalBuilder.build(), targetBuilder.build(), "Target builder should produce the same PID after cloning");

        // Test equals and hashCode
        PIDBuilder builder1 = new PIDBuilder(testSeed).validPrefix().validSuffix();
        PIDBuilder builder2 = new PIDBuilder(testSeed).validPrefix().validSuffix();
        assertEquals(builder1, builder2, "Builders with same configuration should be equal");
        assertEquals(builder1.hashCode(), builder2.hashCode(), "Equal builders should have same hash code");

        // Test toString
        assertNotNull(originalBuilder.toString(), "toString should not return null");
        assertTrue(originalBuilder.toString().contains("PIDBuilder"), "toString should contain class name");
    }

    @Test
    @DisplayName("Test comprehensive PIDRecordBuilder functionality")
    void testPIDRecordBuilderAllMethods() {
        Long testSeed = 67890L;

        // Test default constructor
        PIDRecordBuilder defaultBuilder = new PIDRecordBuilder();
        assertNotNull(defaultBuilder, "Default PIDRecordBuilder constructor should create non-null instance");
        assertNotNull(defaultBuilder.build(), "Default PIDRecordBuilder should build non-null record");

        // Test constructor with PIDBuilder
        PIDBuilder pidBuilder = new PIDBuilder(testSeed).validPrefix().validSuffix();
        PIDRecordBuilder builderWithPid = new PIDRecordBuilder(pidBuilder);
        assertNotNull(builderWithPid, "PIDRecordBuilder with PIDBuilder should create non-null instance");
        assertEquals(pidBuilder.build(), builderWithPid.build().getPid(), "PIDRecordBuilder should use PID from provided PIDBuilder");

        // Test constructor with PIDBuilder and seed
        PIDRecordBuilder builderWithSeed = new PIDRecordBuilder(pidBuilder, testSeed);
        assertNotNull(builderWithSeed, "PIDRecordBuilder with PIDBuilder and seed should create non-null instance");
        assertEquals(testSeed, builderWithSeed.seed, "PIDRecordBuilder should store the provided seed");

        // Test withSeed method
        PIDRecordBuilder seedModified = new PIDRecordBuilder().withSeed(testSeed);
        assertEquals(testSeed, seedModified.seed, "withSeed method should set the correct seed value");

        // Test withPid method
        String customPid = "test/custom-pid";
        PIDRecordBuilder pidModified = new PIDRecordBuilder().withPid(customPid);
        assertEquals(customPid, pidModified.build().getPid(), "withPid method should set the custom PID value");

        // Test the completeProfile method
        PIDRecordBuilder profileBuilder = new PIDRecordBuilder().completeProfile();
        PIDRecord profileRecord = profileBuilder.build();
        assertNotNull(profileRecord, "completeProfile should produce a non-null record");
        assertFalse(profileRecord.getEntries().isEmpty(), "completeProfile should generate record entries");

        // Test incompleteProfile method
        PIDRecordBuilder incompleteBuilder = new PIDRecordBuilder().incompleteProfile();
        PIDRecord incompleteRecord = incompleteBuilder.build();
        assertNotNull(incompleteRecord, "incompleteProfile should produce a non-null record");

        // Test invalidValues method with different parameters
        PIDRecordBuilder invalidValuesBuilder1 = new PIDRecordBuilder().invalidValues(3);
        PIDRecord invalidRecord1 = invalidValuesBuilder1.build();
        assertNotNull(invalidRecord1, "invalidValues(3) should produce a non-null record");

        PIDRecordBuilder invalidValuesBuilder2 = new PIDRecordBuilder().invalidValues(2, "21.T11148/397d831aa3a9d18eb52c");
        PIDRecord invalidRecord2 = invalidValuesBuilder2.build();
        assertNotNull(invalidRecord2, "invalidValues with specific key should produce a non-null record");

        PIDRecordBuilder invalidValuesBuilder3 = new PIDRecordBuilder().invalidValues(0);
        PIDRecord invalidRecord3 = invalidValuesBuilder3.build();
        assertNotNull(invalidRecord3, "invalidValues(0) should produce a non-null record");

        // Test invalidKeys method
        PIDRecordBuilder invalidKeysBuilder = new PIDRecordBuilder().invalidKeys(3);
        PIDRecord invalidKeysRecord = invalidKeysBuilder.build();
        assertNotNull(invalidKeysRecord, "invalidKeys should produce a non-null record");
        assertTrue(invalidKeysRecord.getEntries().size() >= 3, "invalidKeys(3) should generate at least 3 entries");

        // Test emptyRecord method
        PIDRecordBuilder emptyBuilder = new PIDRecordBuilder().completeProfile().emptyRecord();
        PIDRecord emptyRecord = emptyBuilder.build();
        assertNotNull(emptyRecord, "emptyRecord should produce a non-null record");
        assertEquals(0, emptyRecord.getEntries().size(), "emptyRecord should have no entries");

        // Test nullRecord method
        PIDRecordBuilder nullBuilder = new PIDRecordBuilder().nullRecord();
        assertThrows(Exception.class, nullBuilder::build, "nullRecord should throw exception when built");

        // Test withPIDRecord method
        PIDRecord existingRecord = new PIDRecord().withPID("test/existing");
        existingRecord.addEntry("test.key", "test.value");
        PIDRecordBuilder recordBuilder = new PIDRecordBuilder().withPIDRecord(existingRecord);
        assertEquals(existingRecord.getPid(), recordBuilder.build().getPid(), "withPIDRecord should use PID from existing record");

        // Test clone method
        PIDRecordBuilder originalRecordBuilder = new PIDRecordBuilder().completeProfile();
        PIDRecordBuilder clonedRecordBuilder = originalRecordBuilder.clone();
        assertNotSame(originalRecordBuilder, clonedRecordBuilder, "Cloned builder should be a different instance");
        assertEquals(originalRecordBuilder.seed, clonedRecordBuilder.seed, "Cloned builder should have the same seed");

        // Test equals and hashCode
        PIDRecordBuilder builder1 = new PIDRecordBuilder(null, testSeed).completeProfile();
        PIDRecordBuilder builder2 = new PIDRecordBuilder(null, testSeed).completeProfile();
        // Note: equals might not be equal due to random elements, but we test the method exists
        assertEquals(builder1, builder2, "Builders with same configuration should be equal");
        assertNotEquals(0, builder1.hashCode(), "hashCode should not return null");

        // Test toString
        assertNotNull(originalRecordBuilder.toString(), "toString should not return null");
        assertTrue(originalRecordBuilder.toString().contains("PIDRecordBuilder"), "toString should contain class name");
    }

    @Test
    @DisplayName("Test PIDRecordBuilder connection functionality")
    void testPIDRecordBuilderConnections() {
        long testSeed = 111L;

        // Create multiple builders for connection testing
        PIDRecordBuilder builder1 = new PIDRecordBuilder(null, testSeed).completeProfile();
        PIDRecordBuilder builder2 = new PIDRecordBuilder(null, testSeed + 1).completeProfile();
        PIDRecordBuilder builder3 = new PIDRecordBuilder(null, testSeed + 2).completeProfile();

        // Test addConnection method
        String connectionKey = "21.T11148/d0773859091aeb451528";
        builder1.addConnection(connectionKey, false, builder2, builder3);

        PIDRecord connectedRecord = builder1.build();
        assertTrue(connectedRecord.hasProperty(connectionKey), "Record should have the connection property after addConnection");

        // Test addConnection with replacement
        builder1.addConnection(connectionKey, true, builder2);
        PIDRecord replacedRecord = builder1.build();
        assertTrue(replacedRecord.hasProperty(connectionKey), "Record should have connection property after replace");

        // Test addConnection error case
        assertThrows(IllegalArgumentException.class, () ->
                builder1.addConnection(connectionKey, false), "addConnection should throw exception when no builders provided");

        // Test connectRecordBuilders static method with default keys
        List<PIDRecordBuilder> connectedBuilders = PIDRecordBuilder.connectRecordBuilders(
                null, null, false, builder1, builder2, builder3);

        assertEquals(3, connectedBuilders.size(), "connectRecordBuilders should return the same number of builders");

        // Verify connections were established
        for (PIDRecordBuilder builder : connectedBuilders) {
            PIDRecord record = builder.build();
            assertTrue(record.hasProperty("21.T11148/d0773859091aeb451528") ||
                    record.hasProperty("21.T11148/4fe7cde52629b61e3b82"), "Connected records should have forward or backward connection property");
        }

        // Test connectRecordBuilders with custom keys
        PIDRecordBuilder builder4 = new PIDRecordBuilder(null, testSeed + 3).completeProfile();
        PIDRecordBuilder builder5 = new PIDRecordBuilder(null, testSeed + 4).completeProfile();

        List<PIDRecordBuilder> customConnectedBuilders = PIDRecordBuilder.connectRecordBuilders(
                "custom.forward.key", "custom.backward.key", true, builder4, builder5);

        assertEquals(2, customConnectedBuilders.size(), "connectRecordBuilders with custom keys should return the correct number of builders");

        // Test connectRecordBuilders error case
        assertThrows(IllegalArgumentException.class, () ->
                PIDRecordBuilder.connectRecordBuilders(null, null, false, builder1), "connectRecordBuilders should throw exception with single builder");
    }

    @Test
    @DisplayName("Test valid connected records creation")
    void checkValidConnectedRecords() throws Exception {
        // Create connected records using all builder functionality
        long baseSeed = 12345L;

        List<PIDRecord> records = new ArrayList<>();

        // Use different PIDBuilder configurations
        PIDBuilder[] pidBuilders = {
                new PIDBuilder(baseSeed).validPrefix().validSuffix(),
                new PIDBuilder(baseSeed + 1).validPrefix().validSuffix(),
                new PIDBuilder(baseSeed + 2).validPrefix().validSuffix()
        };

        PIDRecordBuilder[] builders = new PIDRecordBuilder[pidBuilders.length];
        for (int i = 0; i < pidBuilders.length; i++) {
            builders[i] = new PIDRecordBuilder(pidBuilders[i], baseSeed + i)
                    .completeProfile()
                    .withSeed(baseSeed + i);
        }

        // Connect the builders
        PIDRecordBuilder.connectRecordBuilders(null, null, false, builders);

        // Build the records
        for (PIDRecordBuilder builder : builders) {
            records.add(builder.build());
        }

        // Submit to API
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(records.size()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").isMap());

        assertEquals(records.size(), knownPidsDao.count(), "Number of stored records should match the number of records submitted");
    }


    @Test
    @DisplayName("Test single valid record creation")
    void testCreateSingleValidRecord() throws Exception {
        // Use comprehensive PIDBuilder configuration
        PIDBuilder pidBuilder = new PIDBuilder(999L)
                .withSeed(999L)
                .validPrefix()
                .validSuffix();

        PIDRecord record = new PIDRecordBuilder(pidBuilder)
                .withSeed(999L)
                .completeProfile()
                .build();

        List<PIDRecord> records = List.of(record);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(1, knownPidsDao.count(), "Exactly one record should be stored in the database");
    }


    @Test
    @DisplayName("Test empty list")
    void testCreateEmptyList() throws Exception {
        List<PIDRecord> emptyRecords = new ArrayList<>();
        String jsonContent = mapper.writeValueAsString(emptyRecords);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        assertEquals(0, knownPidsDao.count(), "No records should be stored in database when submitting an empty list");
    }

    @Test
    @DisplayName("Test dryrun functionality")
    void testDryRun() throws Exception {
        PIDRecord record = new PIDRecordBuilder()
                .completeProfile()
                .build();

        List<PIDRecord> records = List.of(record);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .param("dryrun", "true"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(0, knownPidsDao.count(), "No records should be stored in database when using dryrun mode");
    }

    @Test
    @DisplayName("Test invalid JSON format")
    void testInvalidJsonFormat() throws Exception {
        String invalidJson = "{ invalid json }";

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Test unsupported media type")
    void testUnsupportedMediaType() throws Exception {
        PIDRecord record = new PIDRecordBuilder().completeProfile().build();
        List<PIDRecord> records = List.of(record);
        String content = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Test records with circular references")
    void testCircularReferences() throws Exception {
        // Create builders with circular connections
        PIDRecordBuilder builder1 = new PIDRecordBuilder(null, 100L).completeProfile();
        PIDRecordBuilder builder2 = new PIDRecordBuilder(null, 101L).completeProfile();

        // Create circular reference
        builder1.addConnection("21.T11148/d0773859091aeb451528", false, builder2);
        builder2.addConnection("21.T11148/4fe7cde52629b61e3b82", false, builder1);

        List<PIDRecord> records = List.of(builder1.build(), builder2.build());
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(2, knownPidsDao.count(), "Both circularly connected records should be stored in the database");
    }

    @Test
    @DisplayName("Test records with duplicate temporary PIDs")
    void testDuplicateTemporaryPids() throws Exception {
        // Create records with same PID using same seed
        Long sameSeed = 555L;
        PIDBuilder sameBuilder = new PIDBuilder(sameSeed).validPrefix().validSuffix();

        PIDRecord record1 = new PIDRecordBuilder(sameBuilder.clone(), sameSeed).completeProfile().build();
        PIDRecord record2 = new PIDRecordBuilder(sameBuilder.clone(), sameSeed).completeProfile().build();

        List<PIDRecord> records = List.of(record1, record2);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Test records with missing entries")
    void testRecordsWithMissingEntries() throws Exception {
        // Create an incomplete record
        PIDRecord incompleteRecord = new PIDRecordBuilder()
                .incompleteProfile()
                .build();

        List<PIDRecord> records = List.of(incompleteRecord);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Test large number of connected records")
    void testLargeNumberOfConnectedRecords() throws Exception {
        List<PIDRecordBuilder> builders = new ArrayList<>();

        for (int i = 0; i < LARGE_RECORD_COUNT; i++) {
            PIDBuilder pidBuilder = new PIDBuilder((long) i).validPrefix().validSuffix();
            builders.add(new PIDRecordBuilder(pidBuilder, (long) i).completeProfile());
        }

        // Connect all builders
        PIDRecordBuilder.connectRecordBuilders(null, null, false,
                builders.toArray(new PIDRecordBuilder[0]));

        List<PIDRecord> records = new ArrayList<>();
        for (PIDRecordBuilder builder : builders) {
            records.add(builder.build());
        }

        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(LARGE_RECORD_COUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(LARGE_RECORD_COUNT, knownPidsDao.count(), "All records from large batch should be stored in the database");
    }

    @Test
    @DisplayName("Test records with external references")
    void testRecordsWithExternalReferences() throws Exception {
        PIDRecord record = new PIDRecordBuilder()
                .completeProfile()
                .build();

        // Add external reference
        record.addEntry("21.T11148/d0773859091aeb451528", "externalRef", "external/pid/reference");

        List<PIDRecord> records = List.of(record);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(1, knownPidsDao.count(), "Record with external reference should be stored in the database");
    }


    @Test
    @DisplayName("Test records with mixed connection types")
    void testMixedConnectionTypes() throws Exception {
        PIDRecordBuilder builder1 = new PIDRecordBuilder(null, 200L).completeProfile();
        PIDRecordBuilder builder2 = new PIDRecordBuilder(null, 201L).completeProfile();
        PIDRecordBuilder builder3 = new PIDRecordBuilder(null, 202L).completeProfile();

        // Use different connection keys
        builder1.addConnection("21.T11148/d0773859091aeb451528", false, builder2);
        builder2.addConnection("21.T11148/4fe7cde52629b61e3b82", false, builder3);
        builder3.addConnection(VALID_CONNECTION_KEYS[0], false, builder1);

        List<PIDRecord> records = List.of(builder1.build(), builder2.build(), builder3.build());
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(3, knownPidsDao.count(), "All three records with mixed connection types should be stored in the database");
    }

    @Test
    @DisplayName("Test records with null PIDs")
    void testRecordsWithNullPids() throws Exception {
        PIDRecord record = new PIDRecordBuilder()
                .completeProfile()
                .withPid(null)
                .build();

        List<PIDRecord> records = List.of(record);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());
    }

    @Test
    @DisplayName("Test PID mapping persistence")
    void testPidMappingPersistence() throws Exception {
        PIDRecord record = new PIDRecordBuilder()
                .completeProfile()
                .build();

        List<PIDRecord> records = List.of(record);
        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated());

        assertEquals(1, knownPidsDao.count(), "One record should be stored in the database");

        // Verify the PID was actually stored
        String storedPid = knownPidsDao.findAll().getFirst().getPid();
        assertNotNull(storedPid, "Stored PID should not be null");
        assertFalse(storedPid.isEmpty(), "Stored PID should not be empty");
    }

    @Test
    @DisplayName("Test PIDBuilder edge cases and combinations")
    void testPIDBuilderEdgeCases() {
        // Test various combinations of prefix and suffix methods
        Long seed = 777L;

        // Test unauthorized prefix with valid suffix
        PIDBuilder unauthorizedValid = new PIDBuilder(seed)
                .unauthorizedPrefix()
                .validSuffix();
        String unauthorizedValidPid = unauthorizedValid.build();
        assertTrue(unauthorizedValidPid.startsWith("0.NA/"), "Unauthorized prefix with valid suffix should start with '0.NA/'");

        // Test empty prefix with empty suffix
        PIDBuilder emptyEmpty = new PIDBuilder(seed)
                .emptyPrefix()
                .emptySuffix();
        String emptyEmptyPid = emptyEmpty.build();
        assertEquals("/", emptyEmptyPid, "Empty prefix with empty suffix should result in just '/'");

        // Test invalid characters combinations
        PIDBuilder invalidCombination = new PIDBuilder(seed)
                .invalidCharactersPrefix()
                .invalidCharactersSuffix();
        String invalidPid = invalidCombination.build();
        assertNotNull(invalidPid, "Invalid characters combination should still produce non-null PID");
        assertTrue(invalidPid.contains("/"), "Invalid characters combination should still contain the separator");

        // Test custom prefix with custom suffix
        PIDBuilder customBoth = new PIDBuilder(seed)
                .withPrefix("test.prefix")
                .withSuffix("test-suffix");
        String customPid = customBoth.build();
        assertEquals("test.prefix/test-suffix", customPid, "Custom prefix and suffix should be combined correctly");
    }

    @Test
    @DisplayName("Test PIDRecordBuilder with various invalid configurations")
    void testPIDRecordBuilderInvalidConfigurations() throws Exception {
        // Test record with invalid keys
        PIDRecord invalidKeysRecord = new PIDRecordBuilder()
                .invalidKeys(5)
                .build();

        assertTrue(invalidKeysRecord.getEntries().size() >= 5, "invalidKeys(5) should generate at least 5 entries");

        // Test record with invalid values for specific keys
        PIDRecord invalidSpecificRecord = new PIDRecordBuilder()
                .completeProfile()
                .invalidValues(2, "21.T11148/397d831aa3a9d18eb52c", "21.T11148/8074aed799118ac263ad")
                .build();

        assertNotNull(invalidSpecificRecord, "Invalid values for specific keys should produce non-null record");

        // Test empty record
        PIDRecord emptyRecord = new PIDRecordBuilder()
                .emptyRecord()
                .build();

        assertEquals(0, emptyRecord.getEntries().size(), "emptyRecord should have no entries");

        // Submit invalid records to test API response
        List<PIDRecord> invalidRecords = List.of(invalidKeysRecord);
        String jsonContent = mapper.writeValueAsString(invalidRecords);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Test builder chaining and method combinations")
    void testBuilderChainingCombinations() {
        Long seed = 888L;

        // Test complex PIDBuilder chaining
        PIDBuilder complexBuilder = new PIDBuilder()
                .withSeed(seed)
                .validPrefix()
                .validSuffix()
                .withPrefix("chained.prefix")
                .withSuffix("chained-suffix");

        String complexPid = complexBuilder.build();
        assertEquals("chained.prefix/chained-suffix", complexPid, "Chained builder methods should override previous settings");

        // Test complex PIDRecordBuilder chaining
        PIDRecordBuilder complexRecordBuilder = new PIDRecordBuilder()
                .withSeed(seed)
                .completeProfile()
                .withPid("custom/pid")
                .invalidValues(1, "21.T11148/397d831aa3a9d18eb52c")
                .invalidKeys(1);

        PIDRecord complexRecord = complexRecordBuilder.build();
        assertEquals("custom/pid", complexRecord.getPid(), "Chained record builder should use the custom PID");
        assertFalse(complexRecord.getEntries().isEmpty(), "Chained record builder should generate entries");

        // Test cloning and modification
        PIDRecordBuilder cloned = complexRecordBuilder.clone();
        cloned.withPid("different/pid");

        assertNotEquals(complexRecordBuilder.build().getPid(), cloned.build().getPid(), "Modifying a cloned builder should not affect the original");
    }

    @Test
    @DisplayName("Test multiple record creation with using RECORD_COUNT constant")
    void testMultipleRecordCreationWithRecordCount() throws Exception {
        List<PIDRecord> records = new ArrayList<>();

        // Create multiple records using RECORD_COUNT constant
        for (int i = 0; i < RECORD_COUNT; i++) {
            PIDBuilder pidBuilder = new PIDBuilder((long) i).validPrefix().validSuffix();
            PIDRecord record = new PIDRecordBuilder(pidBuilder, (long) i)
                    .completeProfile()
                    .build();
            records.add(record);
        }

        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(RECORD_COUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(RECORD_COUNT, knownPidsDao.count(), "All RECORD_COUNT records should be stored in the database");
    }

    @Test
    @DisplayName("Test connected records with RECORD_COUNT")
    void testConnectedRecordsWithRecordCount() throws Exception {
        List<PIDRecordBuilder> builders = new ArrayList<>();

        // Create RECORD_COUNT builders for connection testing
        for (int i = 0; i < RECORD_COUNT; i++) {
            PIDBuilder pidBuilder = new PIDBuilder((long) i).validPrefix().validSuffix();
            builders.add(new PIDRecordBuilder(pidBuilder, (long) i).completeProfile());
        }

        // Connect all builders
        PIDRecordBuilder.connectRecordBuilders(null, null, false,
                builders.toArray(new PIDRecordBuilder[0]));

        List<PIDRecord> records = new ArrayList<>();
        for (PIDRecordBuilder builder : builders) {
            records.add(builder.build());
        }

        String jsonContent = mapper.writeValueAsString(records);

        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pidRecords.length()").value(RECORD_COUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$.mapping").exists());

        assertEquals(RECORD_COUNT, knownPidsDao.count(), "All RECORD_COUNT connected records should be stored in the database");
    }

    @Test
    @DisplayName("Test partial failure and rollback scenario")
    void testPartialFailureAndRollback() throws Exception {
        List<PIDRecord> records = new ArrayList<>();

        // Create some valid records
        for (int i = 0; i < 3; i++) {
            PIDRecord validRecord = new PIDRecordBuilder()
                    .completeProfile()
                    .build();
            records.add(validRecord);
        }

        // Add records that should cause validation or creation failures
        // Record with incomplete profile (missing required entries)
        PIDRecord incompleteRecord = new PIDRecordBuilder()
                .incompleteProfile()
                .build();
        records.add(incompleteRecord);

        // Record with invalid keys
        PIDRecord invalidKeysRecord = new PIDRecordBuilder()
                .invalidKeys(3)
                .build();
        records.add(invalidKeysRecord);

        // Record with invalid values for specific keys
        PIDRecord invalidValuesRecord = new PIDRecordBuilder()
                .completeProfile()
                .invalidValues(2, "21.T11148/397d831aa3a9d18eb52c")
                .build();
        records.add(invalidValuesRecord);

        String jsonContent = mapper.writeValueAsString(records);

        // This should result in a server error due to failed validation/ creation,
        // and the rollback mechanism should be triggered
        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // Verify that no PIDs were persisted due to rollback
        assertEquals(0, knownPidsDao.count(), "Rollback should prevent any records from being stored due to partial failures");
    }

    @Test
    @DisplayName("Test batch with mixed valid and invalid records for rollback coverage")
    void testBatchWithMixedRecordsForRollbackCoverage() throws Exception {
        List<PIDRecord> records = new ArrayList<>();

        // Create valid records that would initially succeed
        for (int i = 0; i < RECORD_COUNT / 2; i++) {
            PIDRecord record = new PIDRecordBuilder()
                    .withSeed((long) i)
                    .completeProfile()
                    .build();
            records.add(record);
        }

        // Add records with various failure scenarios to trigger rollback

        // Empty record (no entries)
        PIDRecord emptyRecord = new PIDRecordBuilder()
                .emptyRecord()
                .build();
        records.add(emptyRecord);

        // Record with null PID and incomplete profile
        PIDRecord nullPidRecord = new PIDRecordBuilder()
                .withPid(null)
                .incompleteProfile()
                .build();
        records.add(nullPidRecord);

        // Record with completely invalid data
        PIDRecord invalidRecord = new PIDRecordBuilder()
                .invalidKeys(5)
                .invalidValues(3)
                .build();
        records.add(invalidRecord);

        // Incomplete record with missing required entries
        PIDRecord incompleteRecord = new PIDRecordBuilder()
                .incompleteProfile()
                .build();
        records.add(incompleteRecord);

        String jsonContent = mapper.writeValueAsString(records);

        // Expect server error due to validation failures
        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // Verify rollback: no records should be persisted
        assertEquals(0, knownPidsDao.count(), "Rollback should prevent any records from being stored due to mixed valid and invalid records");
    }

    @Test
    @DisplayName("Test record creation failure scenarios with duplicate PIDs in batch")
    void testRecordCreationFailureWithDuplicatePids() throws Exception {
        List<PIDRecord> records = new ArrayList<>();

        // Create multiple valid records first
        for (int i = 0; i < RECORD_COUNT / 4; i++) {
            PIDRecord record = new PIDRecordBuilder()
                    .withSeed((long) i)
                    .completeProfile()
                    .build();
            records.add(record);
        }

        // Add duplicate PIDs to trigger failure
        String duplicatePid = "sandboxed/duplicate-test-pid";

        PIDRecord record1 = new PIDRecordBuilder()
                .completeProfile()
                .withPid(duplicatePid)
                .build();
        records.add(record1);

        PIDRecord record2 = new PIDRecordBuilder()
                .completeProfile()
                .withPid(duplicatePid)
                .build();
        records.add(record2);

        String jsonContent = mapper.writeValueAsString(records);

        // This should fail due to duplicate PIDs and trigger rollback
        this.mockMvc
                .perform(post("/api/v1/pit/pids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // Verify no records were persisted
        assertEquals(0, knownPidsDao.count(), "Rollback should prevent any records from being stored due to duplicate PIDs");
    }
}