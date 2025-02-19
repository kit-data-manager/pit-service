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

public class PIDBuilder implements Cloneable {
    Long seed;
    private Random random;
    private String prefix;
    private String suffix;

    public PIDBuilder() {
        this(new Random().nextLong());
    }

    public PIDBuilder(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);

        // Default values
        this.validPrefix();
        this.validSuffix();
    }

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

    public PIDBuilder withSeed(Long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        return this;
    }

    public PIDBuilder withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String build() {
        return prefix + "/" + suffix;
    }

    public PIDBuilder clone(PIDBuilder builder) {
        this.seed = builder.seed;
        this.random = new Random(seed);
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        return this;
    }

    public PIDBuilder validPrefix() {
        this.prefix = "sandboxed";
        return this;
    }

    public PIDBuilder unauthorizedPrefix() {
        this.prefix = "0.NA";
        return this;
    }

    public PIDBuilder emptyPrefix() {
        this.prefix = "";
        return this;
    }

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

    public PIDBuilder withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public PIDBuilder validSuffix() {
        // generate a UUID based on the seed
        UUID uuid = generateUUID(seed.toString());
        this.suffix = uuid.toString();
        return this;
    }

    public PIDBuilder emptySuffix() {
        this.suffix = "";
        return this;
    }

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

    @Override
    public String toString() {
        return "PIDBuilder{" +
                "prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", seed=" + seed +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PIDBuilder that)) return false;

        return Objects.equals(prefix, that.prefix) && Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(prefix);
        result = 31 * result + Objects.hashCode(suffix);
        return result;
    }

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
