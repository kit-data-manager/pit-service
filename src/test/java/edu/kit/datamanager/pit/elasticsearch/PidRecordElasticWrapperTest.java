package edu.kit.datamanager.pit.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JacksonException;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.web.ApiMockUtils;

// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class PidRecordElasticWrapperTest {

    @Autowired
    ITypingService typingService;

    @Test
    void testCreateEmpty() {
        PIDRecord pidRecord = new PIDRecord();
        PidRecordElasticWrapper wrapper = new PidRecordElasticWrapper(pidRecord, typingService.getOperations());
        assertNotNull(wrapper);
    }

    @Test
    void testCreateValid() throws JacksonException {
        PIDRecord pidRecord = ApiMockUtils.getSomePidRecordInstance();
        PidRecordElasticWrapper wrapper = new PidRecordElasticWrapper(pidRecord, typingService.getOperations());
        assertNotNull(wrapper);
    }
}
