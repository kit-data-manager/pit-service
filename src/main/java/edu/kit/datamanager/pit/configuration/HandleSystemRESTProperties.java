package edu.kit.datamanager.pit.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("pit.pidsystem.handle")
@Validated
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnBean(value = ApplicationProperties.class)
@ConditionalOnExpression(
    "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).HANDLE_REST.name() }"
)
public class HandleSystemRESTProperties {

    private static final Logger LOG = LoggerFactory.getLogger(HandleSystemRESTProperties.class);

    public HandleSystemRESTProperties() {
        LOG.info("Parse HANDLE_REST configuration details.");
    }

    @Value("${pit.pidsystem.handle.userName}")
    private String handleUser;
  
    @Value("${pit.pidsystem.handle.userPassword}")
    private String handlePassword;
  
    @Value("${pit.pidsystem.handle.generatorPrefix}")
    private String generatorPrefix;

    public String getHandleUser() {
        return handleUser;
    }

    public void setHandleUser(String handleUser) {
        this.handleUser = handleUser;
    }

    public String getHandlePassword() {
        return handlePassword;
    }

    public void setHandlePassword(String handlePassword) {
        this.handlePassword = handlePassword;
    }

    public String getGeneratorPrefix() {
        return generatorPrefix;
    }

    public void setGeneratorPrefix(String generatorPrefix) {
        this.generatorPrefix = generatorPrefix;
    }
}
