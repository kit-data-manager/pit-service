package edu.kit.datamanager.pit.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * JSON representation for pagination using the Tabulator.js library.
 */
public class TabulatorPaginationFormat<T> {

    public TabulatorPaginationFormat(Page<T> page) {
        this.lastPage = page.getTotalPages();
        this.data = page.getContent();
    }

    @JsonProperty("last_page")
    private int lastPage;

    private List<T> data;

    public int getLastPage() {
        return lastPage;
    }

    public List<T> getData() {
        return data;
    }
}
