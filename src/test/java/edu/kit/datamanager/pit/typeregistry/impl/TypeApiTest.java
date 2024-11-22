package edu.kit.datamanager.pit.typeregistry.impl;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import org.everit.json.schema.Schema;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeApiTest {

    private TypeApi dtr;
    protected ApplicationProperties props;

    TypeApiTest() throws MalformedURLException, URISyntaxException {
        ApplicationProperties props = new ApplicationProperties();
        props.setExpireAfterWrite(10);
        props.setMaximumSize(1000);
        props.setTypeRegistryUri(new URI("https://typeapi.lab.pidconsortium.net").toURL());
        this.dtr = new TypeApi(props);
    }

    @Test
    void querySchemaOfComplexType() {
        Schema s = dtr.querySchema("21.T11148/82e2503c49209e987740");
        assertNotNull(s);
    }
}