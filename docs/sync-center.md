Sync Center

Overview
- Global sync surface available from the top app bar overflow menu.
- Centralizes "sync all" and per-source sync so controls are not duplicated.
- Shows compact progress and status without cluttering other screens.

Entry Point
- Open the overflow menu in the top app bar and choose "Sync".

UI Behavior
- Header shows current sync status and last synced time.
- "Sync all now" triggers a full refresh across all sources.
- Search box filters sources by name or URL.
- Type chips filter the source list by RSS, YouTube, or Medium.
- Each source row exposes a "Sync" action for individual refresh.
- While syncing, a progress bar shows completed/total.

Progress and Status
- Sync progress is reported as completed/total.
- The currently syncing source is highlighted in the list.
- Errors are shown as a small status line in the sheet.

Related UX
- Feed supports pull-to-refresh for quick sync.
- Feed shows a compact progress bar while syncing is active.

Implementation Notes
- Sync is coordinated in SyncCenterViewModel.
- Per-source sync uses syncSource(sourceId) in each syncer.
- Global sync uses sequential sync to surface progress.
