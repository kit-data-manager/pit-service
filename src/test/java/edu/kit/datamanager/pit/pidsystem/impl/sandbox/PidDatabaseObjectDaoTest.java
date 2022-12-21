package edu.kit.datamanager.pit.pidsystem.impl.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class PidDatabaseObjectDaoTest {
    @Autowired
    private PidDatabaseObjectDao dao;

    @BeforeEach
    public void setUp() {
        // prepareDataBase();
        PidDatabaseObject a = new PidDatabaseObject("first", "first");
        PidDatabaseObject b = new PidDatabaseObject("second", "second");
        PidDatabaseObject c = new PidDatabaseObject("second", "third");
        dao.deleteAll();
        dao.save(a);
        // this.dao.save(b);
        // c should override b as it has the same pid!
        // this.dao.save(c);
    }

    @Test
    @Transactional
    public void testOverrideMinPidRecords() {
        //MinPidRecord a = new MinPidRecord("first", "first");
        //MinPidRecord b = new MinPidRecord("second", "second");
        //MinPidRecord c = new MinPidRecord("second", "third");
        //this.dao.deleteAll();
        //this.dao.saveAndFlush(a);
        //this.dao.saveAndFlush(b);
        //// c should override b as it has the same pid!
        //this.dao.saveAndFlush(c);

        assertEquals(1, dao.count());
        Optional<PidDatabaseObject> overrider = dao.findById("first");
        assertTrue(overrider.isPresent());
        assertEquals("first", overrider.get().getEntries().entrySet().iterator().next().getKey());
    }
}
