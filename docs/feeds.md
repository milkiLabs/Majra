Majra Feeds Architecture

Overview
- Local-first feed storage using Room.
- Pluggable source types and per-source viewers.
- Manual RSS sync only (no background sync yet).

Core Models
- Source: id, name, type, url.
- Article: id, sourceId, sourceType, title, summary, content, url, author, publishedAtMillis, isSaved, readState.
- ReadState: Unread | Read.

Source Types
- rss (implemented)
- youtube, medium, bluesky (placeholders only)

Data Layer
- Room database: sources and articles tables.
- RoomFeedRepository exposes flows for sources, feed, and saved.
- addSource writes to Room and updates the sources flow.

RSS Sync
- RssSyncer reads all RSS sources from Room.
- Fetches and parses using prof18/rss-parser.
- Builds a stable article id from source id and item guid/link/title/pubDate.
- Preserves isSaved and readState on refresh.
- Manual sync only, triggered from the Sources screen.

UI Integration
- Sources screen shows a Sync RSS button and Add source dialog.
- Add source lets users enter name and URL; source type picker shows future types disabled.
- Feed and Saved lists are driven by repository flows.
- Article detail resolves the viewer from ViewerRegistry.

Viewer System
- ViewerRegistry maps sourceType -> ArticleViewer.
- RssArticleViewer is registered as the RSS viewer.
- Fallback viewer currently reuses the RSS viewer.

Adding a Source
1) Open Sources tab.
2) Tap Add source.
3) Enter URL (name optional).
4) Keep type = RSS (other types are disabled for now).
5) Tap Add, then Sync RSS.

Extending Later
- Implement a new ContentSource/Syncer for a source type.
- Register a viewer for that type in ViewerRegistry.
- Enable the type in the source picker.
- Add parsing and upsert logic to Room.
