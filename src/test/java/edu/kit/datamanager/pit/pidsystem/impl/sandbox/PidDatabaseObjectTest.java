package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JacksonException;

import edu.kit.datamanager.pit.web.ApiMockUtils;

@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class PidDatabaseObjectTest {
    @Autowired
    private PidDatabaseObjectDao dao;

    @BeforeEach
    public void setUp() {
        //prepareDataBase();
        dao.deleteAll();
    }

    @Test
    @Transactional
    void testStorePidDatabaseObject() throws JacksonException {
        PidDatabaseObject dbo = new PidDatabaseObject(ApiMockUtils.getSomePidRecordInstance());
        dao.saveAndFlush(dbo);
    }
}
