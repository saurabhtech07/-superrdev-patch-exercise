package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks in the two bugs fixed in this patch:
 * 1. AND/OR precedence in the search query — archived rows used to leak in via
 *    description matches, and the status filter was ignored for title matches.
 * 2. Invalid input used to produce HTTP 500s (bad status enum, negative page).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchNeverReturnsArchivedTasks() throws Exception {
        // Before the fix, q=api returned archived ids 20 and 21 because the
        // archived filter only bound to the title half of the OR.
        mockMvc.perform(get("/api/tasks").param("q", "api").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(not(0)))
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))));
    }

    @Test
    void statusFilterAppliesToTitleMatchesToo() throws Exception {
        // Before the fix, "Update API rate limiting" (IN_PROGRESS) leaked
        // into results for status=OPEN because it matched on title.
        mockMvc.perform(get("/api/tasks")
                        .param("q", "api")
                        .param("status", "OPEN")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(not(0)))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("OPEN"))));
    }

    @Test
    void invalidStatusReturns400Not500() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "foo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroPageIsClampedToOneInsteadOf500() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items.length()").value(not(0)));
    }

    @Test
    void negativePageIsClampedToOneInsteadOf500() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void zeroPageSizeIsClampedInsteadOfReturningEmptyItems() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.items.length()").value(1));
    }
}
