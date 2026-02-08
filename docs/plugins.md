Majra Source Plugins

Overview
Majra uses a plugin architecture for source types (RSS, Podcasts, YouTube, Medium, etc.).
Each plugin owns:
- Source type metadata (label, icon, input hints, enabled/disabled).
- Input resolution (normalize a user input into a feed URL + display name).
- Sync logic (fetch and upsert articles).
- Optional viewer association (how articles render in the detail screen).

This keeps the core models stable while making new sources easy to add with minimal
central wiring.

Quick Start (5 steps)
1) Create a syncer that fetches and upserts articles into Room.
2) Implement SourcePlugin with label, icon, input hints, resolve, and sync methods.
3) Register the plugin in AppDependencies (plugins list).
4) Provide an ArticleViewer (optional) for detail rendering.
5) Run the app, add a source, and verify sync + detail view.

Key Concepts

SourceTypeId
- A sealed type that replaces string constants.
- Backed by a stable string value, stored in Room via type converters.
- Built-ins: Rss, Podcast, Youtube, Medium, Bluesky.
- Custom: SourceTypeId.Custom("my-source").

Location
- app/src/main/java/com/milkilabs/majra/core/model/ContentModels.kt

SourcePlugin
A plugin implementation provides:
- id: SourceTypeId
- displayName: UI label used across chips and menus.
- icon: UI icon for chips and dropdowns.
- inputMode: UrlOnly or UrlOrHandle.
- inputHint: Placeholder text for the Add Source dialog.
- isEnabled: Whether the type can be added (disabled types show as read-only).
- viewer: Optional ArticleViewer for the detail screen.
- resolve(input): Validates and converts user input into a feed URL + display name.
- syncAll / syncSource: Pulls data and upserts into Room.

Location
- app/src/main/java/com/milkilabs/majra/core/source/ContentSource.kt

SourcePluginRegistry
- A simple registry that exposes plugin lists and metadata for the UI.
- Used by view models and screens to build type chips and labels.
- Central lookup for sync dispatch and input resolution.

Location
- app/src/main/java/com/milkilabs/majra/core/source/ContentSource.kt

ViewerRegistry
- Maps SourceTypeId to an ArticleViewer.
- Populated from plugins in AppDependencies.

Location
- app/src/main/java/com/milkilabs/majra/core/viewer/ViewerRegistry.kt

How It All Wires Together

AppDependencies
- Creates syncers and viewers.
- Creates plugins (RssSourcePlugin, PodcastSourcePlugin, YoutubeSourcePlugin, etc.).
- Builds SourcePluginRegistry from the plugin list.
- Builds ViewerRegistry from plugin.viewer entries.

Location
- app/src/main/java/com/milkilabs/majra/AppDependencies.kt

ManageSourcesViewModel
- Uses SourcePluginRegistry to:
  - Validate input (based on inputMode).
  - Resolve URLs (plugin.resolve).
  - Dispatch per-source sync (plugin.syncSource).
- Exposes typeOptions for UI chips and dialogs.

Location
- app/src/main/java/com/milkilabs/majra/feed/ManageSourcesViewModel.kt

FeedScreen and ManageSourcesSheet
- UI type chips and menus use registry metadata (label + icon).
- Input hints use plugin.inputHint.
- Disabled plugins are visible but not selectable.

Location
- app/src/main/java/com/milkilabs/majra/navigation/screens/FeedScreen.kt
- app/src/main/java/com/milkilabs/majra/navigation/screens/ManageSourcesSheet.kt

Article Model (Current)
Article remains a single model with optional media fields. This means:
- All source types share the same base fields.
- Podcast-specific fields are optional and may be null for non-podcast sources.

Fields
- id, sourceId, sourceType
- title, summary, content, url, author
- audioUrl, audioMimeType, audioDurationSeconds, episodeNumber, imageUrl
- publishedAtMillis, isSaved, readState

Location
- app/src/main/java/com/milkilabs/majra/core/model/ContentModels.kt

Room Storage and Type Conversion
- Room stores SourceTypeId as a string value via converters.
- Sources and articles store SourceTypeId in their type/sourceType columns.

Location
- app/src/main/java/com/milkilabs/majra/data/db/Converters.kt
- app/src/main/java/com/milkilabs/majra/data/db/Entities.kt
- app/src/main/java/com/milkilabs/majra/data/db/AppDatabase.kt

Adding a New Source Plugin (Step-by-Step)

1) Create a syncer (or reuse an existing one)
- Implement fetching and upserting into Room.
- Pattern: RssSyncer, PodcastSyncer, YoutubeSyncer, MediumSyncer.

2) Create a plugin class
- Implement SourcePlugin.
- Provide displayName, icon, inputMode, inputHint, and resolve/sync methods.

Example skeleton:

class RedditSourcePlugin(
    private val syncer: RedditSyncer,
    override val viewer: ArticleViewer,
) : SourcePlugin {
    override val id: SourceTypeId = SourceTypeId.Custom("reddit")
    override val displayName: String = "Reddit"
    override val icon = Icons.Filled.Forum
    override val inputMode: SourceInputMode = SourceInputMode.UrlOrHandle
    override val inputHint: String = "https://reddit.com/r/android"
    override val isEnabled: Boolean = true

    override suspend fun resolve(input: String): SourceResolveResult {
        return syncer.resolve(input)
    }

    override suspend fun syncAll() {
        syncer.syncAll()
    }

    override suspend fun syncSource(sourceId: String) {
        syncer.syncSource(sourceId)
    }
}

3) Register the plugin in AppDependencies
- Add it to the plugins list alongside existing ones.
- If you provide a viewer, it is auto-registered in ViewerRegistry.

Location
- app/src/main/java/com/milkilabs/majra/AppDependencies.kt

4) (Optional) Add a viewer
- Implement ArticleViewer for custom rendering.
- Use viewerRegistry to render the article detail screen.

Location
- app/src/main/java/com/milkilabs/majra/core/viewer/ViewerRegistry.kt

5) Verify UI integration
- Manage Sources and Feed filters should show the new type automatically.
- Input hint and icon should show in the Add Source dialog.

Disabling a Plugin
- Set isEnabled = false to show the type but prevent adding.
- Useful for placeholders like Bluesky.

Testing Tips
- Add a temporary source and confirm it appears in Manage Sources.
- Trigger sync and verify articles appear in the feed.
- Open a detail screen to confirm the viewer renders correctly.

Common Pitfalls
- Forgetting to register the plugin in AppDependencies.
- Mismatch between resolve output URL and source uniqueness logic.
- Not preserving readState or isSaved in syncer upserts.

Related Docs
- docs/feeds.md
- docs/syncing.md
- docs/manage-sources.md
