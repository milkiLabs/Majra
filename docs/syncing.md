# Syncing

This document explains how source syncing works in Majra today. It describes
manual sync behavior, current limitations, and how sync status is surfaced.

## High-level Flow
1) User triggers sync from Manage Sources or feed pull-to-refresh.
2) ManageSourcesViewModel coordinates sync in sequence.
3) Each plugin fetches a feed and upserts articles into Room.
4) UI observes sync status and shows progress or errors.

## Where Sync Starts
- Manage Sources sheet: "Sync all now" or per-source "Sync".
- Feed screen: pull-to-refresh triggers sync all.

## Orchestration
- ManageSourcesViewModel is the sync coordinator.
- Sync runs sequentially for predictable progress updates.
- Sync dispatch uses SourcePluginRegistry for the source type.
- SyncStatus exposes:
  - isSyncing
  - completed/total
  - currentSourceId
  - lastSyncedMillis (last attempt)
  - errorMessage (latest failure)

## Source Types
### RSS
- RssSyncer fetches RSS channel items and upserts them into Room.
- Stable IDs are built from sourceId + guid/link/title/pubDate.
- Read and saved state are preserved on refresh.

### YouTube
- YoutubeSyncer resolves a channel/playlist/handle to an RSS feed URL.
- The resolved feed is parsed like RSS sources.

### Medium
- MediumSyncer resolves handles/publications/custom domains to RSS feed URLs.
- The resolved feed is parsed like RSS sources.

## Data Storage
- Sources and articles are stored in Room.
- Article fields: title, summary, content, url, author,
  audioUrl, audioMimeType, audioDurationSeconds, episodeNumber, imageUrl,
  publishedAtMillis.
- User state: readState and isSaved are preserved when items are refreshed.

## Current Limitations
- Manual-only: no background/periodic sync.
- Sync failures may not surface if parsing fails silently.
- Per-source errors are not persisted; only the last error is shown.

## Future (Planned)
- Periodic background sync using WorkManager.
- Per-source error history and last success timestamps.
- Optional background sync toggle in settings.
