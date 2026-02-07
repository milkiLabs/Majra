# Theming in Majra

Majra uses a preference-driven Material 3 theme with curated warm palettes inspired by earthy, timeless motifs. User choices (mode, palette, typography scale, shape density) are persisted with DataStore Preferences and applied at app start.

## High-Level Flow

1. `ThemePreferencesRepository` exposes a `Flow<ThemePreferences>` from DataStore.
2. `MainActivity` collects the flow and feeds it into `MajraTheme`.
3. `MajraTheme` maps preferences to `colorScheme`, `typography`, and `shapes`.
4. The Settings screen updates preferences, which immediately recompose the app.

## Key Files

- Theme entry point: `app/src/main/java/com/example/majra/ui/theme/Theme.kt`
- Color tokens: `app/src/main/java/com/example/majra/ui/theme/Color.kt`
- Typography scaling: `app/src/main/java/com/example/majra/ui/theme/Type.kt`
- Preferences model: `app/src/main/java/com/example/majra/settings/ThemePreferences.kt`
- DataStore repository: `app/src/main/java/com/example/majra/settings/ThemePreferencesRepository.kt`
- Settings UI controls: `app/src/main/java/com/example/majra/navigation/MajraScreens.kt`

## Preferences Model

`ThemePreferences` contains:
- `mode`: `System`, `Light`, or `Dark`
- `accentPalette`: `Evergreen`, `Cedar`, or `Oasis`
- `typographyScale`: `Small`, `Default`, or `Large`
- `shapeDensity`: `Rounded` or `Sharp`

These are stored as strings in DataStore Preferences, with safe defaults when missing.

## Adding or Adjusting Palettes

1. Define new color tokens in `Color.kt`.
2. Extend `AccentPalette` with a new entry.
3. Add light/dark mappings in `AccentPalette.tokens()` inside `Theme.kt`.
4. Update Settings UI labels if needed.

## Typography Scaling

`typographyForScale()` in `Type.kt` scales all Material text styles uniformly using a small factor (0.9x, 1.0x, 1.1x). This avoids per-style drift while keeping the design consistent.

## Shape Density

`shapesForDensity()` in `Theme.kt` sets Material corner radii for small/medium/large shapes. Rounded uses larger radii, Sharp keeps corners tighter.

## Persistence Details

- Storage: `DataStore<Preferences>` named `theme_preferences`.
- Keys: `theme_mode`, `accent_palette`, `typography_scale`, `shape_density`.
- Repository: `ThemePreferencesRepository` handles read/write and defaults.

## Notes

- The theme is applied globally in `MainActivity` so every screen is consistently styled.
- Dynamic color is intentionally disabled to preserve the curated palette.
