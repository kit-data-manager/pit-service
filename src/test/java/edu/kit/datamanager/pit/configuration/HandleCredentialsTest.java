package edu.kit.datamanager.pit.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HandleCredentialsTest {

    final String noSlash = "withoutSlash";
    final String withSlash = "withoutSlash/";

    @Test
    @DisplayName("Expect handle prefix has a slash, even if it was configured WITHOUT it.")
    void testGetHandleIdentifierPrefixWithoutSlash() {
        HandleCredentials creds = new HandleCredentials();
        creds.setHandleIdentifierPrefix(noSlash);
        assertEquals(withSlash, creds.getHandleIdentifierPrefix());
    }

    @Test
    @DisplayName("Expect handle prefix has a slash, even if it was configured WITH it.")
    void testGetHandleIdentifierPrefixWithSlash() {
        HandleCredentials creds = new HandleCredentials();
        creds.setHandleIdentifierPrefix(withSlash);
        assertEquals(withSlash, creds.getHandleIdentifierPrefix());
    }
}
