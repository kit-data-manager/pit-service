package edu.kit.datamanager.pit.domain.minimal;

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

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class MinPidRecordDaoTest {
    @Autowired
    private MinPidRecordDao dao;

    @BeforeEach
    public void setUp() {
        //prepareDataBase();
    }

    @Test
    public void testOverrideMinPidRecords() {
        MinPidRecord a = new MinPidRecord("first", "first");
        MinPidRecord b = new MinPidRecord("second", "second");
        MinPidRecord c = new MinPidRecord("second", "third");
        this.dao.deleteAll();
        this.dao.saveAndFlush(a);
        this.dao.saveAndFlush(b);
        // c should override b as it has the same pid!
        this.dao.saveAndFlush(c);

        assertEquals(2, dao.count());
        Optional<MinPidRecord> overrider = dao.findById("second");
        assertTrue(overrider.isPresent());
        assertEquals("third", overrider.get().getEntries().iterator().next().getKey());
    }
}
