Majra Feeds Architecture

Overview
- Local-first feed storage using Room.
- Pluggable source types and per-source viewers.
- Manual sync (no background sync yet).

Core Models
- Source: id, name, type, url.
- Article: id, sourceId, sourceType, title, summary, content, url, author, publishedAtMillis, isSaved, readState.
- ReadState: Unread | Read.

Source Types
- rss (implemented)
- youtube (implemented via RSS feeds; handles supported)
- medium (implemented via RSS feeds)
- bluesky (placeholders only)

Data Layer
- Room database: sources and articles tables.
- RoomFeedRepository exposes flows for sources, feed, and saved.
- addSource writes to Room and updates the sources flow.

RSS Sync
- RssSyncer reads all RSS sources from Room.
- Fetches and parses using prof18/rss-parser.
- Builds a stable article id from source id and item guid/link/title/pubDate.
- Preserves isSaved and readState on refresh.
- Manual sync only, triggered from Manage Sources or pull-to-refresh.

UI Integration
- Manage Sources is available from the top app bar overflow menu.
- Feed supports pull-to-refresh for quick sync and shows sync progress.
- Add source lets users enter a URL; RSS sources try to resolve the title automatically.
- Source type picker shows future types disabled.
- Feed and Saved lists are driven by repository flows.
- Article detail resolves the viewer from ViewerRegistry.

Viewer System
- ViewerRegistry maps sourceType -> ArticleViewer.
- RssArticleViewer is registered as the RSS viewer.
- Fallback viewer currently reuses the RSS viewer.

Adding a Source
1) Open the top app bar overflow menu.
2) Tap Manage sources.
3) Tap Add source.
4) Enter a URL.
5) Choose RSS or YouTube.
6) For YouTube, paste a channel handle, channel URL/ID, or playlist URL.
7) For Medium, paste a handle, publication, or RSS feed URL.
	- Custom domains are supported; we'll try to resolve them to /feed URLs.
8) Tap Add, then use Sync all or pull-to-refresh.

Feed Filters
- Read status: Unread | Read | All (default: Unread)
- Source filter: pick a specific source from a searchable sheet
- Source type filter: RSS / YouTube / Medium
- Category chip is a placeholder for future filtering

Manage Sources
- Global sheet reachable from the top app bar overflow menu.
- Sync all button for one-tap refresh.
- Add, edit, and remove actions are available in the source list.
- Per-source sync actions are available in the source list.
- Progress shows completed/total and current source when syncing.
- Feed pull-to-refresh triggers sync all and shows a themed indicator.

Extending Later
- Implement a new ContentSource/Syncer for a source type.
- Register a viewer for that type in ViewerRegistry.
- Enable the type in the source picker.
- Add parsing and upsert logic to Room.
