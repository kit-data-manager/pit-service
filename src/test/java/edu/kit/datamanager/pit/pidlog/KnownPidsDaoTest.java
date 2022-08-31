package edu.kit.datamanager.pit.pidlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource("/test/application-test.properties")
@ActiveProfiles("test")
public class KnownPidsDaoTest {

    @Autowired
    private KnownPidsDao knownPidsDao;
    private KnownPidsDao instance;

    private static final Instant NOW = Instant.now();

    private static final Instant TOO_SOON = NOW.minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant MIN = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant SOONER = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant LATER = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant MAX = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    private static final Instant TOO_LATE = NOW.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

    @BeforeAll
    public static void setUpClass() {

    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        prepareDataBase();
        instance = knownPidsDao;
    }

    @AfterEach
    public void tearDown() {
    }

    private void prepareDataBase() {
        ArrayList<KnownPid> datasets = new ArrayList<>();
        datasets.add(new KnownPid("too_soon2", TOO_SOON.minus(1, ChronoUnit.DAYS), TOO_SOON));
        datasets.add(new KnownPid("too_soon", TOO_SOON, TOO_SOON));
        datasets.add(new KnownPid("sooner", SOONER, SOONER));
        datasets.add(new KnownPid("now", NOW, NOW));
        datasets.add(new KnownPid("later", LATER, LATER));
        datasets.add(new KnownPid("too_late", NOW, NOW));
        // This one shall override the previous, as it was not correct (on purpose):
        datasets.add(new KnownPid("too_late", TOO_LATE, TOO_LATE));
        datasets.add(new KnownPid("too_late2", TOO_LATE, TOO_LATE.plus(1, ChronoUnit.DAYS)));
        knownPidsDao.deleteAll();
        knownPidsDao.saveAllAndFlush(datasets);
    }

    @Test
    void testEntryWasOverridden() {
        Optional<KnownPid> entry = instance.findByPid("too_late");
        assertTrue(entry.isPresent());
        System.out.println("Found: " + entry.get().toString());
        assertEquals(TOO_LATE, entry.get().getCreated());
        assertEquals(TOO_LATE, entry.get().getModified());
    }

    @Test
    void testCountDistinctPidsByCreatedBetween() {
        long num = instance.countDistinctPidsByCreatedBetween(MIN, MAX);
        assertEquals(3, num);
    }

    @Test
    void testCountDistinctPidsByModifiedBetween() {
        long num = instance.countDistinctPidsByModifiedBetween(MIN, MAX);
        assertEquals(3, num);
    }

    @Test
    void testFindByPid() {
        Optional<KnownPid> entry = instance.findByPid("now");
        assertTrue(entry.isPresent());
    }

    @Test
    void testFindDistinctPidsByCreated() {
        Collection<KnownPid> siblings = instance.findDistinctPidsByCreated(TOO_LATE);
        assertEquals(2, siblings.size());
    }

    @Test
    void testFindDistinctPidsByCreatedPageable() {
        Pageable page = PageRequest.ofSize(1).first();
        Page<KnownPid> page_siblings;
        do {
            page_siblings = instance.findDistinctPidsByCreated(TOO_LATE, page);
            assertEquals(2, page_siblings.getTotalElements());
            assertEquals(2, page_siblings.getTotalPages());
            assertEquals(1, page_siblings.getNumberOfElements());
            page = page_siblings.nextPageable();
        } while (page_siblings.hasNext());
    }

    @Test
    void testFindDistinctPidsByCreatedBetween() {
        Collection<KnownPid> pids = instance.findDistinctPidsByCreatedBetween(MIN, MAX);
        assertEquals(3, pids.size());
    }

    @Test
    void testFindDistinctPidsByCreatedBetweenPageable() {
        Pageable page = PageRequest.ofSize(1).first();
        Page<KnownPid> page_siblings;
        do {
            page_siblings = instance.findDistinctPidsByCreatedBetween(MIN, MAX, page);
            assertEquals(3, page_siblings.getTotalElements());
            assertEquals(3, page_siblings.getTotalPages());
            assertEquals(1, page_siblings.getNumberOfElements());
            page = page_siblings.nextPageable();
        } while (page_siblings.hasNext());
    }

    @Test
    void testFindDistinctPidsByModified() {
        Collection<KnownPid> pids = instance.findDistinctPidsByModified(TOO_SOON);
        assertEquals(2, pids.size());
    }

    @Test
    void testFindDistinctPidsByModifiedPageable() {
        Pageable page = PageRequest.ofSize(1).first();
        Page<KnownPid> page_siblings;
        do {
            page_siblings = instance.findDistinctPidsByModifiedBetween(MIN, MAX, page);
            assertEquals(3, page_siblings.getTotalElements());
            assertEquals(3, page_siblings.getTotalPages());
            assertEquals(1, page_siblings.getNumberOfElements());
            page = page_siblings.nextPageable();
        } while (page_siblings.hasNext());
    }

    @Test
    void testFindDistinctPidsByModifiedBetween() {
        Collection<KnownPid> pids = instance.findDistinctPidsByModifiedBetween(MIN, MAX);
        assertEquals(3, pids.size());
    }

    @Test
    void testFindDistinctPidsByModifiedBetweenPageable() {
        Pageable page = PageRequest.ofSize(1).first();
        Page<KnownPid> page_siblings;
        do {
            page_siblings = instance.findDistinctPidsByModifiedBetween(MIN, MAX, page);
            assertEquals(3, page_siblings.getTotalElements());
            assertEquals(3, page_siblings.getTotalPages());
            assertEquals(1, page_siblings.getNumberOfElements());
            page = page_siblings.nextPageable();
        } while (page_siblings.hasNext());
    }
}
