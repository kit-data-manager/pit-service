package edu.kit.datamanager.pit.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import edu.kit.datamanager.pit.common.InvalidConfigException;

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
        if (!this.handleIdentifierPrefix.endsWith("/")) {
            this.handleIdentifierPrefix += "/";
        }
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

    /**
     * Extract bytes from private key.
     * 
     * This only ready the content. It might be encrypted or contain garbage.
     * 
     * @return the bytes fron the private key file.
     * @throws IOException on error when reading from file.
     */
    public byte[] getPrivateKeyFileContent() throws IOException {
        {
            File maybeKeyFile = this.getPrivateKeyPath().toFile();
            if (!maybeKeyFile.exists()) {
                throw new InvalidConfigException(
                        String.format("PrivateKeyFilePath does not lead to a file: %s", maybeKeyFile.toString()));
            }
            if (!maybeKeyFile.isFile()) {
                throw new InvalidConfigException(
                        String.format("File to private key not a regular file: %s", maybeKeyFile.toString()));
            }
        }
        return Files.readAllBytes(this.getPrivateKeyPath());
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    /**
     * Extract and prepare passphrase from configuration.
     * 
     * @return the passphrase as byte sequence.
     */
    @Nullable
    public byte[] getPrivateKeyPassphraseAsBytes() {
        // Passphrase may be null if key is not encrypted, this is intended.
        byte[] passphrase = null;
        String given = this.getPrivateKeyPassphrase();
        if (given != null && !given.isEmpty()) {
            passphrase = given.getBytes(StandardCharsets.UTF_8);
            if (passphrase.length < 1) {
                throw new InvalidConfigException("Passphrase for key file is set but empty!");
            }
        }
        return passphrase;
    }
}
