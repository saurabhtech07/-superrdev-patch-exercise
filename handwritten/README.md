# Handwritten notes — what to write

For **each bug below**, handwrite on paper: (1) where it is, (2) how you found it,
(3) root cause, (4) how you fixed it and why. Then photo/scan the pages and put
the images in this folder (`handwritten/`).

Pictures should be named like `bug1-search-precedence.jpg`, etc.

## Bug 1 — Search query ignores archived + status (SQL precedence)
- Location: `backend/src/main/java/com/internal/tasktracker/TaskRepository.java` (repository `@Query`), also mirrored in `db/queries/search_tasks.sql` and `db/oracle/task_search_package.sql`.
- Discovery: called `/api/tasks?q=api` and saw archived task ids 20/21 in the response; called `?q=api&status=OPEN` and got an IN_PROGRESS row.
- Root cause: `WHERE archived = FALSE AND title LIKE ... OR description LIKE ... AND (status...)` parses as `(archived AND title) OR (description AND status)` because AND binds tighter than OR.
- Fix: wrap the two LIKE tests in parentheses and keep `archived`/`status` outside: `archived = FALSE AND (title LIKE ... OR description LIKE ...) AND (status ...)`. Fixed in all three files.

## Bug 2 — Every request is ~1 second slow
- Location: `TaskController.java`, the "query complexity" block.
- Discovery: timed `curl` — empty query took 1.03s, `q=api` 0.71s, `q=refactor` 0.23s.
- Root cause: `Thread.sleep(queryWeight)` blocks the request thread; the "complexity" was inverted so the most common (empty/short) queries were the slowest.
- Fix: deleted the sleep entirely (nothing in the app needs it).

## Bug 3 — Failed request leaves UI stuck on "Loading..."; stale responses
- Location: `frontend/src/hooks/useTasks.js`.
- Discovery: killed the backend while typing a search — the page stayed on "Loading tasks..." forever and the error message never appeared.
- Root cause: `setLoading(false)` only ran on success, and `TaskTable` checks `loading` before `error`; the `useEffect` also had no cleanup, so out-of-order responses could overwrite newer ones.
- Fix: reset error at request start, always clear loading via `finally`, and mark the request cancelled in the effect cleanup so stale results are ignored.

## Bug 4 — Bad input returns HTTP 500
- Location: `TaskController.java`, status parsing and pagination math.
- Discovery: `curl ?status=foo`, `?page=0`, `?page=-5` all returned 500.
- Root cause: `TaskStatus.valueOf()` throws `IllegalArgumentException` (500), and `subList((page-1)*pageSize, ...)` throws `IndexOutOfBoundsException` for negative/zero page (500).
- Fix: catch the enum error and return 400 with a readable message; clamp `page >= 1` and `1 <= pageSize <= 100`.

## Bug 5 — Page not reset + no debounce (frontend)
- Location: `frontend/src/App.jsx` (+ new `hooks/useDebouncedValue.js`).
- Discovery: on page 3, typed a search — empty result page and pagination controls vanished, leaving the user stuck.
- Root cause: `page` state was not reset when query/status changed; every keystroke also fired a request (one per key).
- Fix: reset `page` to 1 in the query/status change handlers, and debounce the search input by 300ms.

## Improvement — logging hygiene + tests
- Replaced `System.out.println` with a SLF4J logger and removed `console.log`.
- Added `backend/src/test/java/com/internal/tasktracker/TaskSearchTest.java` (6 tests) that fail on the old code and pass on the fixed code.