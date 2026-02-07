# Navigation in Majra

This app uses AndroidX Navigation 3 (Nav3) with a multi-stack setup. Each top-level tab keeps its own back stack, so switching tabs preserves history.

## Key Concepts (Nav3)

- A back stack stores keys, not composables. Keys are small data objects that describe a destination.
- Keys must implement `NavKey` and be `@Serializable` to allow state restoration.
- `NavDisplay` renders the stack by asking an entry provider to map keys to UI.

## Where the Navigation Lives

- App entry and UI wiring: `app/src/main/java/com/example/majra/navigation/MajraApp.kt`
- Keys (routes): `app/src/main/java/com/example/majra/navigation/NavKeys.kt`
- Multi-stack state: `app/src/main/java/com/example/majra/navigation/NavigationState.kt`
- Navigation helper: `app/src/main/java/com/example/majra/navigation/Navigator.kt`

## Architecture Overview

1. `NavKeys.kt` defines all routes as serializable keys (for example, `Feed`, `ContentDetail`).
2. `MajraApp()` creates a `NavigationState` that holds one back stack per top-level tab.
3. `Navigator` mutates the stacks on UI events (push, pop, or tab switch).
4. The entry provider maps keys to their composables.
5. `NavDisplay` renders decorated entries with transitions.

## Top-Level Tabs and Back Behavior

- Tabs are defined in `MajraApp()` with a single metadata list (key, label, title, icon).
- Selecting a tab switches the active back stack without losing its history.
- Selecting the current tab pops its stack to the root screen.
- Back behavior:
  - Pop within the current tab.
  - If already at the tab root, switch back to the start tab.

## Adding a New Screen

1. Add a new `@Serializable` key in `NavKeys.kt`.
2. Add a new entry in the `entryProvider` in `MajraApp.kt`.
3. Use `Navigator.navigate(NewKey(...))` to open it.

If the screen is a new top-level tab:
- Add it to the `topLevelDestinations` list in `MajraApp()` (label/title/icon live there).
- Ensure it is included in `topLevelRoutes` passed to `rememberNavigationState()`.

## Adding Arguments

Use data class keys to pass arguments:

```kotlin
@Serializable
data class ArticleDetail(
    val id: String,
    val title: String,
) : MajraNavKey
```

The entry provider receives the key instance, so you can read its fields directly.

## State and ViewModel Scoping

`NavigationState.toDecoratedEntries()` applies:
- Saveable state holder per entry.
- ViewModel store per entry.

This matches Nav3 guidance so each screen keeps its own state while on the stack.

## Files to Read First

- `app/src/main/java/com/example/majra/navigation/MajraApp.kt`
- `app/src/main/java/com/example/majra/navigation/NavigationState.kt`
- `app/src/main/java/com/example/majra/navigation/Navigator.kt`
- `app/src/main/java/com/example/majra/navigation/NavKeys.kt`
