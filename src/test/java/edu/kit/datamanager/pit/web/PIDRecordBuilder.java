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

import edu.kit.datamanager.pit.domain.PIDRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Builder class for creating PIDRecord instances with various configurations for testing purposes.
 *
 * <p>This class facilitates the generation of PID records with specific properties, connections to other records,
 * and the addition of metadata.
 * It is primarily designed for testing scenarios where different types of
 * PID records are needed, including both valid and invalid configurations.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Create records with valid or invalid data</li>
 *   <li>Connect multiple records in various relationship patterns</li>
 *   <li>Manage the internal state of records</li>
 *   <li>Add metadata, connections, PIDs, and generate invalid values or keys</li>
 *   <li>Create records that conform to or violate specific profiles</li>
 *   <li>Support for deterministic record generation using seeds</li>
 * </ul>
 *
 * <p>This builder relies on the Helmholtz Kernel Information Profile
 * (<a href="https://hdl.handle.net/21.T11148/301c6f04763a16f0f72a">21.T11148/301c6f04763a16f0f72a</a>)
 * and its data types for validation and structure of the records.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     // Create a basic valid record
 *     PIDRecordBuilder builder = new PIDRecordBuilder();
 *     PIDRecord record = builder.withPid("21.T11148/1234567890abcdef")
 *        .completeProfile()
 *        .build();
 *
 *     // Create connected records
 *     PIDRecordBuilder builder1 = new PIDRecordBuilder();
 *     PIDRecordBuilder builder2 = new PIDRecordBuilder();
 *     builder1.addConnection("21.T11148/d0773859091aeb451528", true, builder2);
 *
 *     // Create an invalid record for testing error handling
 *     PIDRecord invalidRecord = new PIDRecordBuilder()
 *        .invalidKeys(3)
 *        .invalidValues(2)
 *        .build();
 * </pre>
 */
public class PIDRecordBuilder implements Cloneable {
    /**
     * Represents the current time plus one minute, truncated to milliseconds.
     * Used for setting timestamps in record metadata to ensure they are in the future.
     */
    private static final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);

    /**
     * Represents the time 24 hours before NOW, truncated to milliseconds.
     * Used for setting creation dates in record metadata to ensure they are before modification dates.
     */
    private static final Instant YESTERDAY = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

    /**
     * PID key that identifies the profile type in a record.
     * This key is used to specify which profile the record conforms to.
     */
    private static final String PROFILE_KEY = "21.T11148/076759916209e5d62bd5";

    /**
     * PID key used for establishing "hasMetadata" relationships between records.
     * This is used when one record references metadata contained in another record.
     */
    private static final String HAS_METADATA_KEY = "21.T11148/d0773859091aeb451528";

    /**
     * PID key used for establishing "isMetadataFor" relationships between records.
     * This is the inverse relationship of HAS_METADATA_KEY.
     */
    private static final String IS_METADATA_FOR_KEY = "21.T11148/4fe7cde52629b61e3b82";

    /**
     * List of standardized PID keys that are required by the Helmholtz Kernel Information Profile.
     * These keys represent mandatory metadata fields that must be present in a valid record:
     * - dateCreated
     * - digitalObjectPolicy
     * - etag
     * - dateModified
     * - digitalObjectLocation
     * - version
     * - digitalObjectType
     */
    private static final List<String> KEYS_IN_PROFILE = new ArrayList<>(Arrays.stream(new String[]{"21.T11148/397d831aa3a9d18eb52c", "21.T11148/8074aed799118ac263ad", "21.T11148/92e200311a56800b3e47", "21.T11148/aafd5fb4c7222e2d950a", "21.T11148/b8457812905b83046284", "21.T11148/c692273deb2772da307f", "21.T11148/c83481d4bf467110e7c9"}).toList());

    /**
     * The seed value used for the random generator to create deterministic records.
     * This enables reproducible test scenarios with consistent record generation.
     */
    Long seed;

    /**
     * Random number generator initialized with the seed value.
     * Used for generating random components of records in a deterministic way.
     */
    private Random random;

    /**
     * The PID record being built by this builder.
     * All operations performed on this builder modify this record instance.
     */
    private PIDRecord record;

    /**
     * Creates a new PIDRecordBuilder with default settings.
     * Initializes a new record with a randomly generated PID using a new random seed.
     * This is the simplest way to create a builder for basic test cases.
     */
    public PIDRecordBuilder() {
        this(null);
    }

    /**
     * Creates a new PIDRecordBuilder using the specified PID builder.
     * If the provided PID builder is null, a default valid PID is generated.
     * This constructor allows control over how the PID for the record is generated.
     *
     * @param pidBuilder PID builder to use to generate a PID for the record, or null to use a default
     */
    public PIDRecordBuilder(PIDBuilder pidBuilder) {
        this(pidBuilder, null);
    }

    /**
     * Creates a new PIDRecordBuilder with the specified PID builder and seed value.
     * This constructor provides full control over both the PID generation and the randomization seed.
     *
     * <p>
     * If the seed is null, a random seed will be generated.
     * If the PID builder is null,
     * a default builder will be created using the seed value to ensure deterministic PID generation.
     * </p>
     *
     * @param pidBuilder PID builder to use to generate a PID for the record, or null to use a default
     * @param seed       Seed value for the random generator, or null to generate a random seed
     */
    public PIDRecordBuilder(PIDBuilder pidBuilder, Long seed) {
        this.seed = seed != null ? seed : new Random().nextLong();
        this.random = new Random(this.seed);
        this.record = new PIDRecord();

        if (pidBuilder == null) {
            pidBuilder = new PIDBuilder(this.seed).validPrefix().validSuffix();
        }
        this.record.setPid(pidBuilder.build());
    }

    /**
     * Creates bidirectional connections between multiple PIDRecordBuilders in a fully meshed network.
     *
     * <p>This static method establishes connections between all provided builders, creating
     * a network where every builder is connected to every other builder. For each pair of builders,
     * two connections are established: one in each direction, using the specified keys.</p>
     *
     * <p>This is particularly useful for testing complex relationship networks between PID records,
     * such as metadata relationships, hierarchical structures, or cross-references.</p>
     *
     * @param a_to_b_key              The key to use for forward connections (from A to B).
     *                                If null, the default "hasMetadata" key is used.
     * @param b_to_a_key              The key to use for backward connections (from B to A).
     *                                If null, the default "isMetadataFor" key is used.
     * @param allowDuplicateRelations Whether to allow multiple connections between the same builders.
     *                                This controls the behavior when adding connections that might already exist.
     * @param builders                Array of PIDRecordBuilders to connect in the network.
     *                                Must contain at least two builders.
     * @return A list containing all the connected builders
     * @throws IllegalArgumentException If fewer than two builders are provided
     */
    public static List<PIDRecordBuilder> connectRecordBuilders(String a_to_b_key, String b_to_a_key, boolean allowDuplicateRelations, PIDRecordBuilder... builders) throws IllegalArgumentException {
        if (builders.length < 2) {
            throw new IllegalArgumentException("At least two builders are required for connection");
        }

        String forwardKey = a_to_b_key != null ? a_to_b_key : HAS_METADATA_KEY;
        String backwardKey = b_to_a_key != null ? b_to_a_key : IS_METADATA_FOR_KEY;

        for (int i = 0; i < builders.length; i++) {
            for (int j = 0; j < builders.length; j++) {
                if (i != j) {
                    builders[i].addConnection(forwardKey, false, builders[j]);
                    builders[j].addConnection(backwardKey, false, builders[i]);
                }
            }
        }

        return Arrays.asList(builders);
    }

    /**
     * Builds and returns the final PID record.
     *
     * <p>This method returns a clone of the internal record, which means the builder can be used
     * to create multiple records with different modifications without affecting previously built records.</p>
     *
     * <p>Example:</p>
     * <pre>
     *   PIDRecordBuilder builder = new PIDRecordBuilder();
     *   PIDRecord record1 = builder.completeProfile().build(); // A valid record
     *   PIDRecord record2 = builder.invalidKeys(2).build();   // An invalid record
     * </pre>
     *
     * @return A new clone of the built PID record
     */
    public PIDRecord build() {
        return this.record.clone();
    }

    /**
     * Sets a new seed value for this builder and reinitializes the random generator.
     *
     * <p>This method allows changing the deterministic behavior of the builder after creation.
     * All later random operations will use the new seed, making test results reproducible
     * when the same seed is used.</p>
     *
     * @param seed The new seed value for random operations
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder withSeed(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        return this;
    }

    /**
     * Sets the PID (Persistent Identifier) of the record being built.
     *
     * <p>This method allows specifying a custom PID instead of using the automatically generated one.
     * This is useful when you need to test with specific, known PIDs or when creating records that
     * need to match existing identifiers.</p>
     *
     * @param pid The PID string to assign to the record
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder withPid(String pid) {
        this.record.setPid(pid);
        return this;
    }

    /**
     * Adds connections from this record to one or more other records using a specified relationship key.
     *
     * <p>This method establishes directional connections from the current record to each of the specified
     * target records. Each connection is made using the provided key, which defines the relationship type.</p>
     *
     * <p>Common relationship types include:</p>
     * <ul>
     *   <li>"21.T11148/d0773859091aeb451528" - hasMetadata relationship</li>
     *   <li>"21.T11148/4fe7cde52629b61e3b82" - isMetadataFor relationship</li>
     * </ul>
     *
     * @param key              The key defining the relationship type for the connections
     * @param replaceIdentical Whether to replace existing connections with the same key and target
     * @param builders         The target PIDRecordBuilders to connect to (at least one is required)
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException If no target builders are provided
     */
    public PIDRecordBuilder addConnection(String key, boolean replaceIdentical, PIDRecordBuilder... builders) throws IllegalArgumentException {
        if (builders.length == 0) {
            throw new IllegalArgumentException("At least one builder is required for connection");
        }
        for (PIDRecordBuilder builder : builders) {
            this.addNotDuplicate(key, builder.record.getPid(), "connectedPID", replaceIdentical);
        }

        return this;
    }

    /**
     * Sets the internal record to a specified PIDRecord instance.
     *
     * <p>This method replaces the current record being built with the provided record instance.
     * This is useful when you want to start with an existing record and make modifications to it,
     * or when you need to restore a builder to a previous state.</p>
     *
     * @param record The PIDRecord to use as the basis for further building
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder withPIDRecord(PIDRecord record) {
        this.record = record;
        return this;
    }

    /**
     * Adds all the required keys and values to make the record conform to the Helmholtz Kernel Information Profile.
     *
     * <p>This method populates the record with a complete set of valid metadata entries that fulfill
     * the requirements of the profile. After calling this method, the record will be valid, according
     * to the profile specifications. The added entries include:</p>
     *
     * <ul>
     *   <li>Profile identifier</li>
     *   <li>Creation date (set to yesterday)</li>
     *   <li>Digital object policy</li>
     *   <li>ETag for versioning</li>
     *   <li>Modification date (set to the current time plus one minute)</li>
     *   <li>Digital object location (a generated URL)</li>
     *   <li>Version information</li>
     *   <li>Digital object type</li>
     * </ul>
     *
     * @return This builder instance for method chaining, now with a valid profile-compliant record
     */
    public PIDRecordBuilder completeProfile() {
        this.addNotDuplicate(PROFILE_KEY, "21.T11148/301c6f04763a16f0f72a", "KernelInformationProfile", true);
        this.addNotDuplicate("21.T11148/397d831aa3a9d18eb52c", YESTERDAY.toString(), "dateCreated", true);
        this.addNotDuplicate("21.T11148/8074aed799118ac263ad", "21.T11148/37d0f4689c6ea3301787", "digitalObjectPolicy", true);
        this.addNotDuplicate("21.T11148/92e200311a56800b3e47", "{ \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }", "etag", true);
        this.addNotDuplicate("21.T11148/aafd5fb4c7222e2d950a", NOW.toString(), "dateModified", true);
        this.addNotDuplicate("21.T11148/b8457812905b83046284", "https://test.example/file001-" + Integer.toHexString(random.nextInt()), "digitalObjectLocation", true);
        this.addNotDuplicate("21.T11148/c692273deb2772da307f", "1.0.0", "version", true);
        this.addNotDuplicate("21.T11148/c83481d4bf467110e7c9", "21.T11148/ManuscriptPage", "digitalObjectType", true);
        return this;
    }

    /**
     * Creates an intentionally incomplete profile by removing mandatory keys.
     *
     * <p>This method first sets a non-standard profile key and then removes all standard profile keys
     * from the record. The resulting record will intentionally fail validation against the standard
     * profile, which is useful for testing error handling and validation logic.</p>
     *
     * <p>Note that even an empty record is, by definition, considered incomplete with respect to
     * the profile requirements. This method ensures the record specifically indicates it follows
     * a different profile than the standard one.</p>
     *
     * @return This builder instance for method chaining, now with an incomplete profile
     */
    public PIDRecordBuilder incompleteProfile() {
        this.addNotDuplicate(PROFILE_KEY, "21.T11148/b9b76f887845e32d29f7", "KernelInformationProfile", true);

        this.record.getEntries().keySet().forEach(key -> {
            if (KEYS_IN_PROFILE.contains(key)) {
                this.record.removeAllValuesOf(key);
            }
        });

        return this;
    }

    /**
     * Adds a specified number of invalid values to the record for testing validation failure scenarios.
     *
     * <p>This method allows fine-grained control over which keys receive invalid values and how many
     * invalid values are added. It can generate invalid values for specific keys or randomly select
     * keys from the standard profile.</p>
     *
     * <p>The behavior depends on the provided parameters:</p>
     * <ul>
     *   <li>If amount ≤ 0 and keys are specified: Generate invalid values for all specified keys</li>
     *   <li>If amount > 0 and keys.length ≥ amount: Generate invalid values for the first 'amount' keys</li>
     *   <li>If amount > 0 and keys.length < amount: Generate invalid values for all specified keys plus
     *       randomly selected keys from the profile until reaching 'amount'</li>
     *   <li>If amount ≤ 0 and no keys specified: Generate invalid values for all keys in the profile</li>
     *   <li>If amount > 0 and no keys specified: Generate 'amount' invalid values for randomly selected keys</li>
     * </ul>
     *
     * @param amount The number of invalid values to add (use 0 or negative for special behavior)
     * @param keys   Optional specific keys for which to generate invalid values
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder invalidValues(int amount, String... keys) {
        List<String> keysToGenerateValuesFor = new ArrayList<>();
        if (amount == 0 && keys.length > 0) {
            keysToGenerateValuesFor.addAll(Arrays.asList(keys));
        } else if (amount > 0 && keys.length >= amount) {
            // add keys with limit of amount
            keysToGenerateValuesFor.addAll(Arrays.asList(keys).subList(0, amount));
        } else if (amount > 0) {
            // add all keys and generate values for the rest
            keysToGenerateValuesFor.addAll(Arrays.asList(keys));
            for (int i = 0; i < amount - keys.length; i++) {
                //Get a random key from the predefined lis
                keysToGenerateValuesFor.add(KEYS_IN_PROFILE.get(random.nextInt(KEYS_IN_PROFILE.size())));
            }
        } else {
            // generate values for all keys
            keysToGenerateValuesFor.addAll(KEYS_IN_PROFILE);
        }

        for (String key : keysToGenerateValuesFor) {
            this.addNotDuplicate(key, "invalid-value-" + random.nextInt(), "key", false);
        }

        return this;
    }

    /**
     * Adds a specified number of randomly generated invalid keys to the record.
     *
     * <p>This method creates nonsensical, randomly generated keys and adds them to the record
     * with random string values. This is useful for testing how systems handle records with
     * unexpected or malformed keys.</p>
     *
     * <p>The generated keys have the prefix "invalid-key-" followed by a random string of
     * characters with a length between 5 and 256. Each key is assigned a random value string
     * of 16 characters.</p>
     *
     * @param amount The number of invalid keys to add to the record
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder invalidKeys(int amount) {
        for (int i = 0; i < amount; i++) {
            this.addNotDuplicate("invalid-key-" + generateRandomString(random.nextInt(5, 256)), generateRandomString(16), "KernelInformationProfile", false);
        }
        return this;
    }

    /**
     * Removes all entries from the record, creating an empty record while preserving the PID.
     *
     * <p>This method is useful for testing how systems handle empty records or for creating
     * a clean slate before adding specific entries. The PID of the record is maintained,
     * but all metadata entries are removed.</p>
     *
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder emptyRecord() {
        String pid = this.record.getPid();
        this.record = new PIDRecord().withPID(pid);
        return this;
    }

    /**
     * Sets the internal record reference to null.
     *
     * <p>This method creates an invalid state where the builder has no record to work with.
     * This is primarily useful for testing null-handling and error recovery in code that uses
     * this builder.</p>
     *
     * <p>Note: After calling this method, most other methods that operate on the record will
     * likely throw NullPointerExceptions if called.</p>
     *
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder nullRecord() {
        this.record = null;
        return this;
    }

    /**
     * Adds an entry to the record, with controls for handling duplicate entries.
     *
     * <p>This method adds a key-value entry to the record. If the key already exists in the record,
     * the behavior depends on the replace parameter:</p>
     * <ul>
     *   <li>If replace is true: Any existing values for the key are removed before adding the new value</li>
     *   <li>If replace is false: The new value is added alongside existing values (may create duplicates)</li>
     * </ul>
     *
     * @param key     The key identifier for the entry
     * @param value   The value to associate with the key
     * @param name    The human-readable name for the entry type
     * @param replace Whether to replace existing values for the key
     * @return This builder instance for method chaining
     */
    public PIDRecordBuilder addNotDuplicate(String key, String value, String name, boolean replace) {
        if (this.record.getEntries().containsKey(key) && replace) {
            this.record.removeAllValuesOf(key);
        }
        this.record.addEntry(key, name, value);
        return this;
    }

    /**
     * Generates a random string of the specified length.
     *
     * <p>This utility method creates a string of random characters, using the full range of
     * possible character values up to Character.MAX_VALUE. This can include characters from
     * any Unicode block, control characters, surrogate pairs, etc.</p>
     *
     * <p>Note that the resulting string may contain characters that are not displayable
     * in all contexts or may cause issues with certain text processing systems.</p>
     *
     * @param length The length of the random string to generate
     * @return A string of random characters with the specified length
     */
    private String generateRandomString(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) random.nextInt(Character.MAX_VALUE);
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Compares this PIDRecordBuilder to another object for equality.
     *
     * <p>Two PIDRecordBuilder instances are considered equal if they have equivalent records.
     * Note that the seed and random generator state are not considered in the equality check,
     * only the record content itself.</p>
     *
     * <p>This method is marked as final to prevent subclasses from changing the equality semantics.</p>
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PIDRecordBuilder that)) return false;

        return Objects.equals(record, that.record);
    }

    /**
     * Returns a hash code value for this PIDRecordBuilder.
     *
     * <p>The hash code is based solely on the record's hash code, consistent with the equals method.
     * This ensures that equal builders have the same hash code, as required by the general contract
     * of the Object.hashCode method.</p>
     *
     * @return The hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(record);
    }

    /**
     * Creates a deep clone of this PIDRecordBuilder.
     *
     * <p>This method creates a new PIDRecordBuilder instance with the same properties as this one,
     * including:</p>
     * <ul>
     *   <li>The same seed value</li>
     *   <li>A new random generator initialized with the same seed</li>
     *   <li>A deep clone of the record being built</li>
     * </ul>
     *
     * <p>This allows for creating independent copies of the builder that can be modified separately
     * without affecting each other, while still maintaining the same initial state.</p>
     *
     * @return A new PIDRecordBuilder instance with the same properties
     * @throws AssertionError If cloning fails, which should never happen since PIDRecordBuilder implements Cloneable
     */
    @Override
    public PIDRecordBuilder clone() {
        try {
            PIDRecordBuilder clone = (PIDRecordBuilder) super.clone();
            clone.seed = this.seed;
            clone.random = new Random(this.seed);
            clone.record = this.record.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Returns a string representation of this PIDRecordBuilder.
     *
     * <p>The string includes the seed value, a reference to the random generator,
     * and the string representation of the record being built. This is primarily
     * useful for debugging and logging purposes.</p>
     *
     * @return A string representation of this builder
     */
    @Override
    public String toString() {
        return "PIDRecordBuilder{" +
                "seed=" + seed +
                ", random=" + random +
                ", record=" + record +
                '}';
    }
}
