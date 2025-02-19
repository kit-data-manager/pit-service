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
import edu.kit.datamanager.pit.domain.PIDRecordEntry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PIDRecordBuilder {
    private static final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant YESTERDAY = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant TOMORROW = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

    Long seed;
    private Random random;
    private PIDRecord record;

    public PIDRecordBuilder() {
        this(null);
    }

    public PIDRecordBuilder(PIDBuilder pidBuilder) {
        this(pidBuilder, new Random().nextLong());
    }

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

    public PIDRecord build() {
        return this.record;
    }

    public PIDRecordBuilder clone(PIDRecordBuilder builder) {
        this.seed = builder.seed;
        this.random = new Random(seed);
        this.record = builder.build();
        return this;
    }

    public PIDRecordBuilder withSeed(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        return this;
    }

    public PIDRecordBuilder withPid(String pid) {
        this.record.setPid(pid);
        return this;
    }

    public PIDRecordBuilder withEntries(Map<String, List<PIDRecordEntry>> entries) {
        this.record.setEntries(entries);
        return this;
    }

    public PIDRecordBuilder completeProfile() {
        this.addNotDuplicate("21.T11148/076759916209e5d62bd5", "21.T11148/301c6f04763a16f0f72a", "KernelInformationProfile", true);
        this.addNotDuplicate("21.T11148/397d831aa3a9d18eb52c", YESTERDAY.toString(), "dateCreated", true);
        this.addNotDuplicate("21.T11148/8074aed799118ac263ad", "21.T11148/37d0f4689c6ea3301787", "digitalObjectPolicy", true);
        this.addNotDuplicate("21.T11148/92e200311a56800b3e47", "{ \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }", "etag", true);
        this.addNotDuplicate("21.T11148/aafd5fb4c7222e2d950a", NOW.toString(), "dateModified", true);
        this.addNotDuplicate("21.T11148/b8457812905b83046284", "https://test.example/file001-" + Integer.toHexString(random.nextInt()), "digitalObjectLocation", true);
        this.addNotDuplicate("21.T11148/c692273deb2772da307f", "1.0.0", "version", true);
        this.addNotDuplicate("21.T11148/c83481d4bf467110e7c9", "21.T11148/ManuscriptPage", "digitalObjectType", true);
        return this;
    }

    public PIDRecordBuilder incompleteProfile() {
        this.addNotDuplicate("21.T11148/076759916209e5d62bd5", "21.T11148/301c6f04763a16f0f72a", "KernelInformationProfile", true);
        this.addNotDuplicate("21.T11148/397d831aa3a9d18eb52c", YESTERDAY.toString(), "dateCreated", true);
        this.addNotDuplicate("21.T11148/8074aed799118ac263ad", "21.T11148/37d0f4689c6ea3301787", "digitalObjectPolicy", true);
        return this;
    }

    public PIDRecordBuilder invalidValues(int amount, String... keys) {
        String[] availableKeys = {"21.T11148/076759916209e5d62bd5", "21.T11148/397d831aa3a9d18eb52c", "21.T11148/8074aed799118ac263ad", "21.T11148/92e200311a56800b3e47", "21.T11148/aafd5fb4c7222e2d950a", "21.T11148/b8457812905b83046284", "21.T11148/c692273deb2772da307f", "21.T11148/c83481d4bf467110e7c9"};

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
                keysToGenerateValuesFor.add(availableKeys[random.nextInt(availableKeys.length)]);
            }
        } else {
            // generate values for all keys
            keysToGenerateValuesFor.addAll(Arrays.asList(availableKeys));
        }

        for (String key : keysToGenerateValuesFor) {
            this.addNotDuplicate(key, "invalid-value-" + random.nextInt(), "key", false);
        }

        return this;
    }

    public PIDRecordBuilder invalidKeys(int amount) {
        for (int i = 0; i < amount; i++) {
            this.addNotDuplicate("invalid-key-" + generateRandomString(random.nextInt(5, 256)), generateRandomString(16), "KernelInformationProfile", false);
        }
        return this;
    }

    public PIDRecordBuilder emptyRecord() {
        String pid = this.record.getPid();
        this.record = new PIDRecord();
        return this;
    }

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

    private String generateRandomString(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) random.nextInt(Character.MAX_VALUE);
            result.append(c);
        }
        return result.toString();
    }
}
