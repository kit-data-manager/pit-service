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

public class PIDRecordBuilder implements Cloneable {
    private static final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant YESTERDAY = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final String PROFILE_KEY = "21.T11148/076759916209e5d62bd5";
    private static final List<String> KEYS_IN_PROFILE = new ArrayList<>(Arrays.stream(new String[]{"21.T11148/397d831aa3a9d18eb52c", "21.T11148/8074aed799118ac263ad", "21.T11148/92e200311a56800b3e47", "21.T11148/aafd5fb4c7222e2d950a", "21.T11148/b8457812905b83046284", "21.T11148/c692273deb2772da307f", "21.T11148/c83481d4bf467110e7c9"}).toList());

    Long seed;
    private Random random;
    private PIDRecord record;

    /**
     * Create a PID record builder with a new PID builder and a new seed.
     */
    public PIDRecordBuilder() {
        this(null);
    }

    /**
     * Create a PID record builder with a given PID builder. If the PID builder is null, a valid PID is generated.
     *
     * @param pidBuilder PID builder to use to generate a PID for the record
     */
    public PIDRecordBuilder(PIDBuilder pidBuilder) {
        this(pidBuilder, new Random().nextLong());
    }

    /**
     * Create a PID record builder with a given seed and a PID builder. If the PID builder is null, a valid PID is generated.
     *
     * @param pidBuilder PID builder to use to generate a PID for the record
     * @param seed       seed for the random generator
     */
    public PIDRecordBuilder(PIDBuilder pidBuilder, Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        this.record = new PIDRecord();
        if (pidBuilder != null) {
            this.record.setPid(pidBuilder.build());
        } else {
            this.record.setPid(new PIDBuilder(seed).validPrefix().validSuffix().build());
        }
    }

    /**
     * Build the PID record.
     * The record is cloned before it is returned.
     * This means that the builder can be used to build multiple records.
     *
     * @return the cloned PID record
     */
    public PIDRecord build() {
        return this.record.clone();
    }

    /**
     * Set the seed for the random generator.
     *
     * @param seed seed to set
     * @return this builder
     */
    public PIDRecordBuilder withSeed(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        return this;
    }

    /**
     * Set the record to a given PID.
     *
     * @param pid PID to set
     * @return this builder
     */
    public PIDRecordBuilder withPid(String pid) {
        this.record.setPid(pid);
        return this;
    }

    /**
     * Set the record to a given PID record.
     *
     * @param record PID record to set
     * @return this builder
     */
    public PIDRecordBuilder withPIDRecord(PIDRecord record) {
        this.record = record;
        return this;
    }

    /**
     * Add valid keys and values to the record that fulfill a predefined profile. If this is the first build step after construction, the PID record is valid.
     *
     * @return this builder (with valid PID record)
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
     * This method removes a random (mandatory) key from the profile.
     * The profile is incomplete afterward.
     * If nothing is in the record, the profile is, per definition, not fulfilled.
     *
     * @return this builder
     */
    public PIDRecordBuilder incompleteProfile() {
        this.addNotDuplicate(PROFILE_KEY, "21.T11148/301c6f04763a16f0f72a", "KernelInformationProfile", true);

        Set<String> containedKeys = new HashSet<>();
        this.record.getEntries().keySet().forEach(key -> {
            if (KEYS_IN_PROFILE.contains(key)) {
                containedKeys.add(key);
            }
        });
        if (!containedKeys.isEmpty()) {
            int randomIndex = random.nextInt(containedKeys.size());
            this.record.removeAllValuesOf(containedKeys.toArray()[randomIndex].toString());
        }
        return this;
    }

    /**
     * Add a given number of invalid values to the record.
     * If the amount is smaller or equal to zero and no keys are specified, invalid values for a predefined list of valid keys (currently length 7) are generated.
     * If you specify keys, the invalid values are generated for these keys.
     * If the amount is greater than the number of keys, the remaining invalid values are generated for randomly selected keys from the predefined list.
     * If the amount is greater than zero and no keys are specified, invalid values are generated for all predefined keys which are randomly repeated until the amount is reached.
     *
     * @param amount number of invalid values to add (optional)
     * @param keys   keys for which invalid values should be generated (optional)
     * @return this builder
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
     * Add a given amount of invalid keys to the record. The keys are generated randomly.
     *
     * @param amount amount of invalid keys to add
     * @return this builder
     */
    public PIDRecordBuilder invalidKeys(int amount) {
        for (int i = 0; i < amount; i++) {
            this.addNotDuplicate("invalid-key-" + generateRandomString(random.nextInt(5, 256)), generateRandomString(16), "KernelInformationProfile", false);
        }
        return this;
    }

    /**
     * Set the record to an empty record. The PID is not changed. All entries are removed.
     *
     * @return this builder
     */
    public PIDRecordBuilder emptyRecord() {
        String pid = this.record.getPid();
        this.record = new PIDRecord();
        return this;
    }

    /**
     * Set the record to null.
     *
     * @return this builder
     */
    public PIDRecordBuilder nullRecord() {
        this.record = null;
        return this;
    }

    /**
     * Add an entry to the record if it does not exist yet.
     *
     * @param key     key of the entry
     * @param value   value of the entry
     * @param replace if true, replace the value of the entry if it already exists, even if it is a list of values
     */
    private void addNotDuplicate(String key, String value, String name, Boolean replace) {
        if (replace == null) replace = false;
        if (this.record.getEntries().containsKey(key) && replace) {
            this.record.removeAllValuesOf(key);
        }
        this.record.addEntry(key, name, value);
    }

    /**
     * Generate a random string of a given length.
     *
     * @param length length of the string
     * @return random string
     */
    private String generateRandomString(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) random.nextInt(Character.MAX_VALUE);
            result.append(c);
        }
        return result.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PIDRecordBuilder that)) return false;

        return Objects.equals(record, that.record);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(record);
    }

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
}
