# Contributing to Majra

**Majra** (Arabic for "stream") is a private, calm, intentional Instagram reader for Android. It lets you manually follow a small set of accounts, sync their posts via OkHttp + HTML scraping, cache everything locally in Room, and read them in a native Jetpack Compose interface — no algorithmic feed, no engagement bait, no notifications.

---

## Architecture Overview

Majra follows a **single-activity MVVM** architecture with **manual dependency injection** (no Hilt/Dagger). The layers are:

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose)                     │
│  FeedScreen / LoginScreen / Profile     │
│  FeedViewModel                          │
├─────────────────────────────────────────┤
│  Repository Layer                       │
│  FeedRepository + FeedSourceClient      │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  InstagramHttpClient  InstagramHtmlParser│
│  Room DB                SessionStore    │
└─────────────────────────────────────────┘
```

### Layers in detail

**UI Layer** (`ui/`)

- Composable screens that observe state from `FeedViewModel`.
- No business logic — screens are pure rendering.
- Uses Material 3, Coil (images), Media3/ExoPlayer (video).

**ViewModel** (`ui/feed/FeedViewModel.kt`)

- Single `FeedViewModel` for the whole app.
- Combines three reactive flows: session auth state, feed posts, and accounts list.
- Exposes a single `FeedUiState` data class consumed by the UI.
- Manages sync/load-older actions via coroutines.

**Repository** (`data/repository/FeedRepository.kt`)

- `FeedRepository` orchestrates sync, pagination, and session management.
- `FeedSourceClient` is an interface for platform-specific sync implementations.
- Only `InstagramFeedSourceClient` is implemented; the interface exists for future Facebook/X/RSS support.
- `SyncResult` is a sealed interface (`Success` / `Failure`).

**Data Layer** (`data/`)

- `InstagramHttpClient` — OkHttp client with three fetch methods: profile HTML, profile JSON (GraphQL API), and paginated user feed JSON.
- `InstagramHtmlParser` — Extracts embedded JSON from Instagram HTML using regex patterns. Walks JSON trees to find user profiles and post nodes.
- `SessionStore` — DataStore-based persistence for session cookies and user agents.
- `Room DB` — Two entities: `AccountEntity` and `PostEntity` with a foreign key relationship and cascade delete.

**Media Layer** (`media/`)

- `VideoPlaybackController` wraps ExoPlayer with reactive state flow. Handles play/pause, speed, quality (Auto/DataSaver/SD/HD/FullHD), PiP, fullscreen.
- `PlaybackService` is a Media3 foreground service for background audio playback.

---

## Project Structure

```
app/src/main/java/com/milki/majra/
├── AppContainer.kt               # Manual DI: wires all dependencies
├── MainActivity.kt               # Single activity, Compose host, PiP/fullscreen
├── data/
│   ├── db/                       # Room database layer
│   │   ├── AccountEntity.kt      # DB entity for social accounts
│   │   ├── InstagramDao.kt       # DAO: feed observation, CRUD, pagination
│   │   ├── MajraDatabase.kt      # Room database (version 2)
│   │   ├── PostEntity.kt         # DB entity for posts (FK → accounts)
│   │   ├── PostMediaConverters.kt # TypeConverters for Platform enum + media list
│   │   └── PostWithAccount.kt    # Relationship class (post + account)
│   ├── local/
│   │   └── SessionStore.kt       # DataStore-based session cookie storage
│   ├── model/
│   │   ├── FeedItem.kt           # Combined post + account
│   │   ├── Platform.kt           # Enum: INSTAGRAM, FACEBOOK, X, RSS
│   │   ├── SocialPost.kt         # Post data model with media items
│   │   └── SocialProfile.kt      # Account/profile data model
│   ├── network/
│   │   ├── InstagramHttpClient.kt    # OkHttp fetcher (HTML + JSON endpoints)
│   │   └── InstagramUserAgent.kt     # Centralized UA strategy
│   ├── repository/
│   │   └── FeedRepository.kt         # Repository + FeedSourceClient + impl
│   └── scraper/
│       └── InstagramHtmlParser.kt    # HTML/JSON parser (regex + JSON tree walk)
├── media/
│   ├── PlaybackService.kt            # Media3 foreground service
│   └── VideoPlaybackController.kt    # ExoPlayer wrapper with reactive state
├── navigation/
│   └── NavRoutes.kt                  # Navigation 3 routes
└── ui/
    ├── feed/
    │   ├── FeedScreen.kt             # Main feed: header, source shelf, post list
    │   └── FeedViewModel.kt          # ViewModel + FeedUiState
    ├── login/
    │   └── LoginScreen.kt            # WebView-based Instagram login
    ├── profile/
    │   └── ProfilePostsScreen.kt     # Per-account post list
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Key Technical Decisions

### 1. Manual DI over Hilt/Dagger

Dependencies are wired in `AppContainer.kt` using plain constructors. This keeps the build simple and avoids annotation processing overhead for a single-module app.

### 2. OkHttp + HTML scraping over official API

The app does NOT use the Instagram Graph API. Instead:

- It uses a **WebView for one-time login** to capture the session cookie.
- It fetches profile HTML and JSON via **OkHttp** with the captured cookie.
- It parses embedded JSON from `<script type="application/json">` tags using **regex**.
- It accesses Instagram's internal web API endpoints (`/api/v1/users/web_profile_info/`, `/api/v1/feed/user/{id}/`) with a hardcoded web app ID.

### 3. Regex-based HTML parsing

The parser (`InstagramHtmlParser.kt`) extracts JSON from HTML using two regex patterns, then walks the JSON tree to find user objects and post nodes. This is brittle — it will break if Instagram changes its HTML structure. The long-term plan is to add a **hidden WebView fallback scraper** (see `notes/future-plan.md`).

### 4. Room as offline-first cache

Posts and accounts are stored locally in SQLite via Room. The feed is served entirely from the local database. Syncing is manual (user-triggered), not automatic.

### 5. Navigation 3 (not Compose Navigation)

The project uses the experimental `androidx.navigation3` library for type-safe navigation with a manual backstack. Routes are simple data objects: `HomeRoute`, `LoginRoute`, `ProfileRoute`.

### 6. Single ViewModel

A single `FeedViewModel` manages all app state. Profile-specific posts are queried via a separate `Flow` from the repository. This works for the current MVP but may need splitting as the app grows.

### 7. Extensible platform design

The `Platform` enum includes `FACEBOOK`, `X`, and `RSS` alongside `INSTAGRAM`, and `FeedSourceClient` is designed to be extended. Only Instagram is implemented.

---

## Data Flow

### Sync Flow (user enters a username)

```
User enters "@username" → taps "Fetch"
  → FeedViewModel.sync()
    → FeedRepository.syncSource()
      → InstagramFeedSourceClient.syncProfile()
        → InstagramHttpClient.fetchProfileJson(username)  // try JSON API first
            → fallback: fetchProfileHtml(username)          // fallback to HTML
        → InstagramHtmlParser.parseProfile(html, feedJson)  // extract posts
      → DAO.upsertAccount() + DAO.upsertPosts()            // persist to Room
    → SyncResult.Success / Failure
  → UI state updates → Snackbar shows result
```

### Pagination Flow (user taps "Older")

```
User taps "Older" on a source card
  → FeedViewModel.loadOlder()
    → FeedRepository.loadOlderPosts()
      → InstagramFeedSourceClient.loadOlderPosts()
        → InstagramHttpClient.fetchUserFeedJson(userId, maxId)
        → InstagramHtmlParser.parseFeedPage()
      → DAO.upsertPosts() + DAO.upsertAccount() (update nextPageToken)
    → SyncResult.Success / Failure
```

### Feed Rendering Flow

```
Room (posts + accounts tables)
  → DAO.observeFeed() : Flow<List<PostWithAccount>>
    → FeedRepository maps to List<FeedItem>
      → FeedViewModel combines with session + accounts
        → FeedUiState (consumed by FeedScreen)
          → LazyColumn renders PostCard composables
```

### Session Flow

```
LoginScreen (WebView)
  → onSessionCaptured(cookie, userAgent)
    → FeedRepository.saveSession()
      → SessionStore.save()  (DataStore)
        → OkHttp reads session via SessionStore.current()
```

---

## Testing

### Unit Tests (`app/src/test/`)

- JUnit 4 tests with no Android framework dependencies.
- Currently only boilerplate (`ExampleUnitTest.kt`).
- Ideal location for parser tests with JSON fixtures.

### Instrumented Tests (`app/src/androidTest/`)

- Run on device/emulator with `@RunWith(AndroidJUnit4.class)`.
- `InstagramHtmlParserTest.kt` is the most substantial test file — it covers:
  - Profile JSON parsing from HTML
  - Separate feed JSON parsing
  - Video items
  - Carousels (sidecar children + carousel_media array)
  - Device test using a saved HTML fixture

### Running Tests

```bash
# All unit tests
./gradlew test

# All instrumented tests
./gradlew connectedAndroidTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.milki.majra.InstagramHtmlParserTest"
```

### What Needs Work

See `notes/future-plan.md` for the full roadmap. High-priority areas:

- **Parser resilience**: Handle more Instagram HTML shapes, add structured error types.
- **Pagination**: End-to-end testing of `loadOlderPosts`.
- **Session UX**: Clear session, re-login, show auth state clearly.
- **Hidden WebView fallback**: Add `InstagramWebViewScraper` for when OkHttp fails.
- **Media resolution**: Better video URL extraction, carousel handling.
- **Tests**: More parser tests with fixtures, ViewModel tests, UI tests.
- **Account management**: Remove accounts, clear cached data.

---

## Learning Resources

- [Jetpack Compose documentation](https://developer.android.com/compose)
- [Room database](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [OkHttp](https://square.github.io/okhttp/)
- [Coil image loading](https://coil-kt.github.io/coil/)
- [Media3 / ExoPlayer](https://developer.android.com/media/media3)
- [Navigation 3](https://developer.android.com/develop/navigation/navigation-3)
