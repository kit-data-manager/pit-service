package edu.kit.datamanager.pit.configuration;


import java.nio.file.Path;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;


@Validated
@Configuration
public class HandleCredentials {
    @NotBlank
    private String userHandle;

    @NotBlank
    private String handleIdentifierPrefix;

    @Min(1)
    private int privateKeyIndex = 300;
    
    @NotNull
    private Path privateKeyPath;
    
    // We get the passphrase out of an environment variable
    // instead of a path to a file containing the passphrase in a properties file.
    // Optional, as key might be unencrypted.
    @Value("#{environment.handleProtocolPrivateKeyPassphrase}")
    private String privateKeyPassphrase;

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getHandleIdentifierPrefix() {
        return handleIdentifierPrefix;
    }

    public void setHandleIdentifierPrefix(String handleIdentifierPrefix) {
        this.handleIdentifierPrefix = handleIdentifierPrefix;
    }

    public int getPrivateKeyIndex() {
        return privateKeyIndex;
    }

    public void setPrivateKeyIndex(int privateKeyIndex) {
        this.privateKeyIndex = privateKeyIndex;
    }

    public Path getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(Path privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }
}
