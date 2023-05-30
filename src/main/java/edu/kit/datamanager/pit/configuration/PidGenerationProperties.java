package edu.kit.datamanager.pit.configuration;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidgeneration.generators.HexChunksGenerator;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenLowerCase;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenPrefixed;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenUpperCase;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenUuid4;

@Validated
@ConfigurationProperties("pit.pidgeneration")
@Configuration
public class PidGenerationProperties {

    enum Mode {
        UUID4,
        HEX_CHUNKS
    }

    enum Case {
        LOWER, UPPER, UNMODIFIED
    }

    @NotNull
    private Mode mode = Mode.UUID4;

    @NotNull
    private Case casing = Case.LOWER;

    /**
     * If a CHUNK-based generation method is being used, this determines the amounts
     * of chunks being used. Otherwise, this setting is being ignored.
     * Chunk length depends on the generator. See their documentation for more
     * information.
     */
    private int numChunks = 4;

    @NotNull
    private Optional<String> brandingPrefix = Optional.empty();

    private boolean customClientPidsEnabled = false;

    /**
     * Creates a {@link PidSuffixGenerator} bean from the given configuration.
     * 
     * @return a {@link PidSuffixGenerator} as defined by the configuration.
     */
    @Bean
    public PidSuffixGenerator pidGenerator() {
        PidSuffixGenerator generator = new PidSuffixGenUuid4();
        if (this.mode == Mode.HEX_CHUNKS) {
            generator = new HexChunksGenerator(this.numChunks);
        } // new generators have to be set up here

        if (casing == Case.LOWER) {
            generator = new PidSuffixGenLowerCase(generator);
        } else if (casing == Case.UPPER) {
            generator = new PidSuffixGenUpperCase(generator);
        }

        // we assume the branding should not be affected
        // by the lower/upper case generators.
        if (brandingPrefix.isPresent()) {
            generator = new PidSuffixGenPrefixed(generator, brandingPrefix.get());
        }

        return generator;
    }

    public boolean isCustomClientPidsEnabled() {
        return customClientPidsEnabled;
    }

    public Optional<String> getBrandingPrefix() {
        return brandingPrefix;
    }

    public void setBrandingPrefix(Optional<String> brandingPrefix) {
        this.brandingPrefix = brandingPrefix;
    }

    public void setCustomClientPidsEnabled(boolean customClientPidsEnabled) {
        this.customClientPidsEnabled = customClientPidsEnabled;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setCasing(Case lowerCase) {
        this.casing = lowerCase;
    }

    public void setNumChunks(int numChunks) {
        this.numChunks = numChunks;
    }

}
