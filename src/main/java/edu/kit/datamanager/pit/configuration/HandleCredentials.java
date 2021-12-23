package edu.kit.datamanager.pit.configuration;


import java.nio.file.Path;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Validated
@Data
@EqualsAndHashCode
@Configuration
public class HandleCredentials {
    @NotBlank
    private String userHandle;

    @Min(1)
    private int privateKeyIndex = 300;
    
    @NotNull
    private Path privateKeyPath;
    
    // We get the passphrase out of an environment variable
    // instead of a path to a file containing the passphrase in a properties file.
    @Value("#{environment.handleProtocolPrivateKeyPassphrase}")
    private String privateKeyPassphrase;
}
