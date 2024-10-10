package edu.kit.datamanager.pit.configuration;

import edu.kit.datamanager.pit.recordDecoration.CopyAttribute;
import edu.kit.datamanager.pit.recordDecoration.RecordModifier;
import jakarta.validation.constraints.NotNull;
import net.handle.hdllib.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.handle.hdllib.Common.STD_TYPE_URL;

@Component
@ConfigurationProperties("pit.pidsystem.handle-protocol")
@Validated
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnBean(value = ApplicationProperties.class)
@ConditionalOnExpression(
    "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).HANDLE_PROTOCOL.name() }"
)
public class HandleProtocolProperties {

    private static final Logger LOG = LoggerFactory.getLogger(HandleProtocolProperties.class);

    // Without given credentials, PIDs can only be resolved.
    // Someone decided not to support Optional in the case of nested properties.
    // See: https://github.com/spring-projects/spring-boot/issues/15999
    // A bad decision is also a decision, I guess.
    @NestedConfigurationProperty
    @Nullable
    private HandleCredentials credentials;

    @Value("#{${pit.pidsystem.handle-protocol.handleRedirectAttributes}}")
    @NotNull
    protected List<String> handleRedirectAttributes = List.of();

    public HandleProtocolProperties() {
        LOG.info("Parse HANDLE_PROTOCOL configuration details.");
    }

    public Collection<RecordModifier> getConfiguredModifiers() {
        Set<RecordModifier> result = new HashSet<RecordModifier>();
        if (!this.handleRedirectAttributes.isEmpty()) {
            result.add(new CopyAttribute(this.handleRedirectAttributes, Util.decodeString(STD_TYPE_URL)));
        }
        return result;
    }

    public HandleCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(HandleCredentials credentials) {
        this.credentials = credentials;
    }
}
