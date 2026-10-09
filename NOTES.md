# NOTES.md

## Summary of changes
- **Removed an artificial `Thread.sleep()` in `TaskController`** that added ~100–1,000ms latency to every request (worst for empty searches).
- **Fixed AND/OR operator-precedence** in the search query (backend repository + both SQL reference files in `db/`): the archived and status filters are now applied to the whole search, so archived tasks no longer leak in via description matches and the status filter is no longer ignored for title matches.
- **Fixed the frontend `useTasks` hook**: a failed request no longer leaves the UI stuck on "Loading..." (error now shows and clears), and stale/out-of-order responses can no longer overwrite newer results.
- **Invalid input now returns HTTP 400 or clamped values** instead of 500 (`status=foo`, `page=0/-1`, `pageSize=0`).
- Added a **300ms debounce** on the search box and **reset pagination to page 1** when the search/filter changes (previously users could end up on an empty page with no way back).
- Replaced `System.out.println` with SLF4J logging and removed debug `console.log`.
- Added **6 integration tests** covering the fixed behaviour (requires a new `spring-boot-starter-test` test dependency).

## What I chose not to change
- **In-memory pagination** (fetch-all then slice) — acceptable at ~50 rows; moving to a `LIMIT/OFFSET` + count query is the right change once the table grows.
- **LIKE wildcard escaping** (`%`/`_` in a search term) — a real but rare edge case; noted as future work.
- **`status` stored as String, the redundant `@CrossOrigin`, and the Spring Boot upgrade** — out of scope for a focused patch.

## Biggest remaining risk
Fetch-all-then-slice pagination: every search materialises the full result set, so latency and memory grow linearly with the table. The project also had no tests at all before this patch, making regressions invisible.

## Tools used
Used an AI coding agent to explore the code, reproduce every bug live with `curl`, and draft the fixes and tests. I reviewed each change and re-verified all fixes by running the app and re-running the same probes.