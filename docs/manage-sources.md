Manage Sources

Overview
- Global source management surface available from the top app bar overflow menu.
- Centralizes add, edit, remove, and sync controls in one place.
- Shows compact progress and status without cluttering other screens.

Entry Point
- Open the overflow menu in the top app bar and choose "Manage sources".

UI Behavior
- Header shows current sync status and last synced time.
- "Sync all now" triggers a full refresh across all sources.
- "Add source" opens a dialog driven by plugin metadata.
- Search box filters sources by name or URL.
- Type chips are populated from registered plugins (label + icon).
- Each source row exposes sync, edit, and remove actions.
- While syncing, a progress bar shows completed/total.

Progress and Status
- Sync progress is reported as completed/total.
- The currently syncing source is highlighted in the list.
- Errors are shown as a small status line in the sheet.

Related UX
- Feed supports pull-to-refresh for quick sync.
- Feed shows a compact progress bar while syncing is active.

Implementation Notes
- Manage Sources is coordinated in ManageSourcesViewModel.
- Per-source sync dispatches through SourcePluginRegistry.
- Global sync uses sequential sync to surface progress.
- Add source resolves URLs via plugin.resolve(input).
