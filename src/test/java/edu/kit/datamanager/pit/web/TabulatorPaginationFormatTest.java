package edu.kit.datamanager.pit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class TabulatorPaginationFormatTest {
    @Test
    void testConstructorAssignments() {
        List<String> list = new ArrayList<>();
        list.add("test1");
        list.add("test2");
        Page<String> page = new PageImpl<String>(list);
        TabulatorPaginationFormat<String> f = new TabulatorPaginationFormat<>(page);
        assertEquals(1, f.getLastPage());
        assertEquals(list, f.getData());
    }
}
