package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import edu.kit.datamanager.pit.domain.PIDRecord;

@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
class PidDatabaseObjectDaoTest {
    @Autowired
    private PidDatabaseObjectDao dao;

    @BeforeEach
    public void setUp() {
        PidDatabaseObject a = new PidDatabaseObject("first", "first");
        PidDatabaseObject b = new PidDatabaseObject("second", "second");
        PidDatabaseObject c = new PidDatabaseObject("second", "third");
        dao.deleteAll();
        dao.save(a);
        this.dao.save(b);
        // c should override b as it has the same pid!
        this.dao.save(c);
        this.dao.flush();
    }

    @Test
    @Transactional
    void testOverridePidInDatabase() {
        assertEquals(2, dao.count());
        // The overridden record has the same pid as "second":
        Optional<PidDatabaseObject> overrider = dao.findById("second");
        assertTrue(overrider.isPresent());
        assertEquals("second", overrider.get().getPid());

        // but the attribute pair is ("third", "third") instead of ("second", "second"):
        Entry<String, ArrayList<String>> e = overrider.get().getEntries().entrySet().iterator().next();
        assertEquals("third", e.getKey());
        assertEquals("third", e.getValue().get(0));
    }

    @Test
    @Transactional
    void testModifyRecord() {
        Optional<PidDatabaseObject> dbo = dao.findById("first");
        PIDRecord rec = new PIDRecord(dbo.get());
        assertEquals(
            rec.getPropertyIdentifiers().iterator().next(),
            dbo.get().getEntries().keySet().iterator().next()
        );

        PidDatabaseObject newDbo = new PidDatabaseObject(rec);
        assertFalse(newDbo.getPid().isEmpty());
        this.dao.saveAndFlush(newDbo);
    }
}
