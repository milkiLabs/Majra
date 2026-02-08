# Sync Sources Audit

Date: 2026-02-08
Scope: Source sync orchestration, UI surfaces, syncers, URL resolution, data persistence, and docs.

## Current Behavior Overview
- Sync is manual-only; no background scheduling.
- Sync entry points: Manage Sources sheet and Feed pull-to-refresh.
- Sync orchestration is centralized in ManageSourcesViewModel.
- Each source type uses an RSS-based syncer that fetches and upserts articles.
- Read/saved state is preserved during refresh.

Key components:
- ManageSourcesViewModel: app/src/main/java/com/milkilabs/majra/feed/ManageSourcesViewModel.kt
- UI surfaces: app/src/main/java/com/milkilabs/majra/navigation/ManageSourcesSheet.kt
  and app/src/main/java/com/milkilabs/majra/navigation/screens/FeedScreen.kt
- Syncers: rss/RssSyncer.kt, youtube/YoutubeSyncer.kt, medium/MediumSyncer.kt
- Resolvers: youtube/YoutubeUrlResolver.kt, medium/MediumUrlResolver.kt
- Storage: data/db/Dao.kt, data/repository/RoomFeedRepository.kt

## Findings (ordered by severity)

### High
1) Sync failures are silently swallowed and rarely surface in UI.
   - All syncers wrap parser calls with runCatching and return empty results on failure.
   - ManageSourcesViewModel wraps syncSourceInternal in runCatching, but syncers do not throw.
   - Result: UI often reports success even when network or parsing fails.
   - Impact: User trust issue, hard-to-debug failures, false "last synced" signal.

2) Potential SQL variable limit crash on large feeds.
   - syncers call articleDao.getByIds(candidates.map { it.id }) in one query.
   - SQLite has a default parameter limit (commonly 999).
   - Feeds with >999 items can cause "too many SQL variables" runtime error.

### Medium
3) Pull-to-refresh can get stuck if sync does not start.
   - FeedScreen sets isPullRefreshing = true and calls onRefresh.
   - If sync is blocked (already syncing or no sources), syncStatus does not change,
     so the refresh indicator can remain active indefinitely.

4) Input validation rejects valid YouTube/Medium URLs without scheme.
   - ManageSourcesViewModel requires isValidUrl for YouTube/Medium unless input starts with "@".
   - Inputs like "youtube.com/@handle" or "medium.com/@handle" are rejected
     even though resolvers can normalize them.

5) Error reporting is only last-error, no per-source history.
   - SyncStatus stores one errorMessage, overwritten by last failure.
   - Users lose visibility into which sources failed during a multi-source sync.

### Low
6) Dedupe keys can collide or drift for some feeds.
   - stableKey falls back to title or pubDate; collisions or unstable pubDate values
     can cause duplicate or missing items over time.

7) No throttling or caching for handle resolution.
   - YouTube handle resolution can issue many HTTP requests per add.
   - No cache means repeated adds or edits re-fetch unnecessarily.

## Proposed Changes (no implementation yet)

### Correctness and Reliability
- Return explicit Result from syncers (success/failure with reason) instead of swallowing.
- Provide per-source error status (map of sourceId -> lastError), keep lastSyncedMillis
  for any attempt as per policy.
- Surface partial failures in both Manage Sources and Feed pull-to-refresh.
- Batch articleDao.getByIds in chunks (e.g., 500) or add a DAO method that
  upserts and preserves user state without large IN lists.

### UX and Validation
- Allow YouTube/Medium URLs without scheme by relaxing isValidUrl checks or
  normalizing input before validation.
- Ensure pull-to-refresh resets even when sync is not started (e.g., when already
  syncing or no sources).

### Data and Parsing
- Prefer guid and link over title/pubDate; consider falling back to a hash of
  multiple fields to reduce collisions.
- Optional: drop or mark items with missing identifiers instead of silently
  skipping (depending on UX preference).

## WorkManager Plan (Periodic Sync)

### Goals
- Enable periodic background sync with constraints (network, battery).
- Keep sync logic single-sourced to avoid duplication.
- Preserve manual sync behavior and UI progress for user-triggered sync.

### Proposed Architecture
1) Introduce a SyncWorker that calls a single sync coordinator entry point.
   - Create a SyncCoordinator interface with syncAll(): Result.
   - ManageSourcesViewModel uses the same coordinator.

2) Configure WorkManager periodic work
   - Interval: 6-12 hours (configurable).
   - Constraints: network connected, battery not low (optional).
   - Backoff: exponential on failure.

3) Provide opt-in setting
   - Settings screen toggle for background sync.
   - Store preference and schedule/cancel work accordingly.

4) Observability
   - Store lastAttemptMillis and lastSuccessMillis in Room or DataStore.
   - Log errors per source for diagnostics.

5) Testing
   - Unit tests for SyncCoordinator and syncers.
   - WorkManager test using TestListenableWorkerBuilder.

### Migration Steps
- Step 1: Refactor syncers to return Result per source.
- Step 2: Add SyncCoordinator and reuse in ManageSourcesViewModel.
- Step 3: Add WorkManager periodic scheduling and settings.
- Step 4: Add persistent sync metadata (last success per source).
- Step 5: Update docs and UI to show background sync status.

## Open Questions
- Should failed sources block the global "lastSynced" timestamp or only show
  per-source failure while still updating the global attempt timestamp?
- Do we want to remove articles that disappear from feeds?
- Should we dedupe across sources for identical URLs?

## Suggested Test Scenarios
- Sync all with mixed failures (network off for one source).
- Per-source sync while a global sync is running.
- Feed with >999 items.
- YouTube handle input without scheme.
- RSS feed with missing guid/link/title/pubDate.
- Pull-to-refresh when no sources exist or sync already in progress.
