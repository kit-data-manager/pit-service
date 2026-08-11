package edu.kit.datamanager.pit.web;

import java.util.Map;

import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExtendedErrorAttributesTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ExtendedErrorAttributes errorAttributes;

    @Test
    public void testPidRecordPresence() {
        // Create a mock request to pass to the error attributes
        HttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);

        // Simulate an exception to be handled by the error attributes
        request.setAttribute("jakarta.servlet.error.exception", new RecordValidationException(
                new PIDRecord().withPID("asdfg"),
                "Validation failed"
        ));

        // Get the error attributes
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());

        // Check if the custom attribute is present
        assertTrue(attributes.containsKey("pid-record"));
    }

    @Test
    public void testBeanRegistration() {
        // Check if the ExtendedErrorAttributes bean is registered
        ExtendedErrorAttributes extendedErrorAttributes = webApplicationContext.getBean(ExtendedErrorAttributes.class);
        assertNotNull(extendedErrorAttributes, "ExtendedErrorAttributes bean should be registered");
    }
}