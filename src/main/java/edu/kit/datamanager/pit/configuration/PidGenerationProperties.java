package edu.kit.datamanager.pit.configuration;

import java.util.Optional;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import edu.kit.datamanager.pit.pidgeneration.PidSuffixGenerator;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenLowerCase;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenPrefixed;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenUpperCase;
import edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenUuid4;

@Validated
@ConfigurationProperties("pit.pidgeneration")
@Configuration
public class PidGenerationProperties {

    enum Mode {
        UUID4
    }

    @NotNull
    private Mode mode = Mode.UUID4;

    @NotNull
    private boolean lowerCase = true;

    @NotNull
    private Optional<String> brandingPrefix = Optional.empty();

    /**
     * Creates a {@link PidSuffixGenerator} bean from the given configuration.
     * 
     * @return a {@link PidSuffixGenerator} as defined by the configuration.
     */
    @Bean
    public PidSuffixGenerator pidGenerator() {
        PidSuffixGenerator generator = new PidSuffixGenUuid4();
        // Add mode modes like this:
        //if (this.mode == Mode.UUID4) {
        //    generator = new PidSuffixGenUuid4();
        //} else if (...) { ... }
        
        if (lowerCase) {
            generator = new PidSuffixGenLowerCase(generator);
        } else {
            generator = new PidSuffixGenUpperCase(generator);
        }
        // we assume the branding should not be affected
        // by the lower/upper case generators.
        if (brandingPrefix.isPresent()) {
            generator = new PidSuffixGenPrefixed(generator, brandingPrefix.get());
        }

        return generator;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setLowerCase(boolean lowerCase) {
        this.lowerCase = lowerCase;
    }

    public void setBrandingPrefix(Optional<String> brandingPrefix) {
        this.brandingPrefix = brandingPrefix;
    }
}
