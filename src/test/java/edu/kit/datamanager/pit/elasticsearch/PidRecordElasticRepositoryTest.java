package edu.kit.datamanager.pit.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JacksonException;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.web.ApiMockUtils;

@SpringBootTest
@TestPropertySource(
        locations = "/test/application-test.properties",
        properties = {
                "repo.search.enabled: true",
                "management.health.elasticsearch.enabled: true"
        }
)
@ActiveProfiles({"test", "elastic"})
@Disabled("We need an instance of elasticsearch for these tests")
class PidRecordElasticRepositoryTest {

    @Autowired
    private PidRecordElasticRepository dao;

    @Autowired
    private ITypingService typingService;

    @BeforeEach
    void setUp() {
        dao.deleteAll();
    }

    @Test
    @Transactional
    void testEmpty() {
        assertEquals(0, dao.count());
    }

    @Test
    @Transactional
    void testStorage() throws JacksonException {
        PIDRecord r = ApiMockUtils.getSomePidRecordInstance();
        PidRecordElasticWrapper w = new PidRecordElasticWrapper(r, typingService.getOperations());
        assertEquals(0, dao.count());
        dao.save(w);
        assertEquals(1, dao.count());
    }

    @Test
    @Transactional
    void testMultipleValues() throws JacksonException {
        PIDRecord r = ApiMockUtils.getSomePidRecordInstance();
        r.addEntry(
            r.getPropertyIdentifiers().stream().findFirst().get(), 
            "", 
            "testing");
        PidRecordElasticWrapper w = new PidRecordElasticWrapper(r, typingService.getOperations());
        assertEquals(0, dao.count());
        dao.save(w);
        assertEquals(1, dao.count());
    }

    @Test
    @Transactional
    void testStorageWithDateNull() throws JacksonException {
        PIDRecord r = new PIDRecord()
                .withPID("not-a-pid")
                .addEntry("21.T11148/076759916209e5d62bd5", "", "21.T11148/b9b76f887845e32d29f7");
        PidRecordElasticWrapper w = new PidRecordElasticWrapper(r, typingService.getOperations());
        assertEquals(0, dao.count());
        dao.save(w);
        assertEquals(1, dao.count());
    }
}
