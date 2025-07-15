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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Builder class for creating Persistent Identifier (PID) strings.
 *
 * <p>This class facilitates the generation of PIDs in various formats for testing purposes. It can create
 * valid PIDs conforming to standard patterns, as well as intentionally invalid PIDs for negative testing
 * scenarios. The builder uses a seed-based approach to generate deterministic PIDs, allowing for
 * reproducible test cases.</p>
 *
 * <p>A PID consists of two parts separated by a slash:
 * <ul>
 *   <li>prefix: typically identifies the naming authority or domain (e.g., "sandboxed")</li>
 *   <li>suffix: a unique identifier (typically a UUID)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *   // Create a valid PID
 *   PIDBuilder builder = new PIDBuilder();
 *   String validPid = builder.build(); // Results in something like "sandboxed/550e8400-e29b-41d4-a716-446655440000"
 *
 *   // Create a PID with custom parts
 *   String customPid = new PIDBuilder()
 *       .withPrefix("test-authority")
 *       .withSuffix("custom-id-123")
 *       .build(); // Results in "test-authority/custom-id-123"
 *
 *   // Create an invalid PID for testing error handling
 *   String invalidPid = new PIDBuilder()
 *       .invalidCharactersPrefix()
 *       .build();
 * </pre>
 */
public class PIDBuilder implements Cloneable {
    /**
     * The seed value used for the random generator to create deterministic PIDs.
     * This enables reproducible test scenarios with consistent PID generation.
     */
    Long seed;

    /**
     * Random number generator initialized with the seed value.
     * Used for generating random components of PIDs in a deterministic way.
     */
    private Random random;

    /**
     * The prefix part of the PID, representing the naming authority or domain.
     * For example, "sandboxed" or "0.NA".
     */
    private String prefix;

    /**
     * The suffix part of the PID, representing the unique identifier.
     * Typically a UUID or other unique string.
     */
    private String suffix;

    /**
     * Creates a new PIDBuilder with a random seed.
     * The builder is initialized with valid default prefix and suffix values.
     */
    public PIDBuilder() {
        this(new Random().nextLong());
    }

    /**
     * Creates a new PIDBuilder with the specified seed.
     * The builder is initialized with valid default prefix and suffix values.
     * Using a specific seed allows for reproducible PID generation across test runs.
     *
     * @param seed The seed value for the random generator
     */
    public PIDBuilder(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);

        // Default values
        this.validPrefix();
        this.validSuffix();
    }

    /**
     * Generates a deterministic UUID based on a seed string.
     * This method uses SHA-1 to hash the seed string and constructs a UUID from the hash.
     * The resulting UUID is consistent for the same input seed.
     *
     * @param seed The seed string to generate the UUID from
     * @return A UUID derived from the hash of the seed string
     * @throws RuntimeException If the SHA-1 algorithm is not available
     */
    private static UUID generateUUID(String seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a new seed value for this builder and reinitializes the random generator.
     * This allows changing the deterministic behavior of the builder after creation.
     *
     * @param seed The new seed value
     * @return This builder instance for method chaining
     */
    public PIDBuilder withSeed(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        return this;
    }

    /**
     * Sets a custom prefix for the PID.
     * The prefix typically represents the naming authority or domain.
     *
     * @param prefix The prefix string to use
     * @return This builder instance for method chaining
     */
    public PIDBuilder withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Builds the final PID string by combining the prefix and suffix with a separator.
     * The resulting format is "prefix/suffix".
     *
     * @return The complete PID string
     */
    public String build() {
        return prefix + "/" + suffix;
    }

    /**
     * Copies all properties from another PIDBuilder instance to this one.
     * This method changes the current builder to match the state of the provided builder.
     *
     * @param builder The source PIDBuilder to copy from
     * @return This builder instance for method chaining
     */
    public PIDBuilder clone(PIDBuilder builder) {
        this.seed = builder.seed;
        this.random = new Random(seed);
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        return this;
    }

    /**
     * Sets a valid prefix for testing purposes.
     * The default valid prefix is "sandboxed", which is used for test environments.
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder validPrefix() {
        this.prefix = "sandboxed";
        return this;
    }

    /**
     * Sets a prefix that would cause authorization issues in a real environment.
     * The prefix "0.NA" is typically reserved and would require special permissions.
     * This is useful for testing authorization error handling.
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder unauthorizedPrefix() {
        this.prefix = "0.NA";
        return this;
    }

    /**
     * Sets an empty prefix, which would result in an invalid PID format.
     * This is useful for testing validation error handling.
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder emptyPrefix() {
        this.prefix = "";
        return this;
    }

    /**
     * Generates a prefix containing invalid characters.
     * Valid prefixes should match the regex pattern: ^[a-zA-Z0-9.-]+$
     * This method generates a string with random characters that deliberately
     * fail this validation, which is useful for testing error handling.
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder invalidCharactersPrefix() {
        // generate a random String not fulfilling this regex: ^[a-zA-Z0-9.-]+$
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < random.nextInt(256); i++) { // Random length
            // generate a random character that is not a letter, number, dot or hyphen
            char c;
            do {
                c = (char) random.nextInt(Character.MAX_VALUE); // Random character
            } while (Character.isLetterOrDigit(c) || c == '.' || c == '-'); // Continue until an invalid character is found
            result.append(c);
        }
        this.prefix = result.toString();
        return this;
    }

    /**
     * Sets a custom suffix for the PID.
     * The suffix is the unique identifier part of the PID.
     *
     * @param suffix The suffix string to use
     * @return This builder instance for method chaining
     */
    public PIDBuilder withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    /**
     * Generates a valid suffix using a UUID derived from the current seed.
     * This ensures that the suffix is both valid and deterministic based on the seed value.
     * The generated UUID follows the standard format (e.g., "550e8400-e29b-41d4-a716-446655440000").
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder validSuffix() {
        // generate a UUID based on the seed
        UUID uuid = generateUUID(seed.toString());
        this.suffix = uuid.toString();
        return this;
    }

    /**
     * Sets an empty suffix, which would result in an invalid PID format.
     * This is useful for testing validation error handling.
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder emptySuffix() {
        this.suffix = "";
        return this;
    }

    /**
     * Generates a suffix containing invalid characters.
     * Valid suffixes should match the regex pattern: ^[a-f0-9-]+$
     * This method attempts to generate a string that doesn't match this pattern,
     * which is useful for testing validation error handling.
     *
     * <p>Note: The implementation appears to have a logical error as it generates
     * characters that DO match the pattern rather than characters that DON'T match it.
     * The condition should be reversed for correct behavior.</p>
     *
     * @return This builder instance for method chaining
     */
    public PIDBuilder invalidCharactersSuffix() {
        // generate a random String not fulfilling this regex: ^[a-f0-9-]+$
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < random.nextInt(256); i++) { // Random length
            // generate a random character that is not a letter, number or hyphen
            char c;
            do {
                c = (char) random.nextInt(Character.MAX_VALUE); // Random character
            } while (Character.digit(c, 16) == -1 && c != '-'); // Continue until an invalid character is found
            result.append(c);
        }
        this.suffix = result.toString();
        return this;
    }

    /**
     * Returns a string representation of this PIDBuilder instance.
     * Includes the prefix, suffix, and seed values for debugging purposes.
     *
     * @return A string representation of this builder
     */
    @Override
    public String toString() {
        return "PIDBuilder{" +
                "prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", seed=" + seed +
                '}';
    }

    /**
     * Compares this PIDBuilder to another object for equality.
     * Two PIDBuilder instances are considered equal if they have the same prefix and suffix.
     * Note that the seed value is intentionally not considered in the equality check.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PIDBuilder that)) return false;

        return Objects.equals(prefix, that.prefix) && Objects.equals(suffix, that.suffix);
    }

    /**
     * Returns a hash code value for this PIDBuilder.
     * The hash code is based on the prefix and suffix values, consistent with the equals method.
     *
     * @return The hash code value
     */
    @Override
    public int hashCode() {
        int result = Objects.hashCode(prefix);
        result = 31 * result + Objects.hashCode(suffix);
        return result;
    }

    /**
     * Creates a deep clone of this PIDBuilder instance.
     * The cloned builder will have the same seed, prefix, and suffix values,
     * but with a new random generator instance initialized with the same seed.
     *
     * @return A new PIDBuilder instance with the same properties
     * @throws AssertionError If cloning fails, which should never happen since PIDBuilder implements Cloneable
     */
    @Override
    public PIDBuilder clone() {
        try {
            PIDBuilder clone = (PIDBuilder) super.clone();
            clone.seed = this.seed;
            clone.random = new Random(this.seed);
            clone.prefix = this.prefix;
            clone.suffix = this.suffix;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
