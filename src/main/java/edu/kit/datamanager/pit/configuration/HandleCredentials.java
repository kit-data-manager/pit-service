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
    /**
     * The handle PID of the user / account to create PIDs.
     */
    @NotBlank
    private String userHandle;

    /**
     * The prefix for the PIDs to be created.
     */
    @NotBlank
    private String handleIdentifierPrefix;

    /**
     * The index of the user handle record, in which the public key is stored.
     */
    @Min(1)
    private int privateKeyIndex = 300;

    /**
     * The path on disk where the file containing the private key of the user is
     * being stored. The file content may be encryptet or not.
     */
    @NotNull
    private Path privateKeyPath;

    // We get the passphrase out of an environment variable
    // instead of a path to a file containing the passphrase in a properties file.
    // Optional, as key might be unencrypted.
    /**
     * The passphrase for the key file stored at privateKeyPath.
     * 
     * We get the passphrase out of an environment variable instead of a path to a
     * file containing the passphrase in a properties file. Optional, as key might
     * be unencrypted.
     */
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
