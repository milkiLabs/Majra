# Agent Guide for Majra

Project
- Android app (single module: :app)
- Kotlin + Jetpack Compose, Gradle Kotlin DSL

Key paths
- App entry: `app/src/main/java/com/milkilabs/majra/MainActivity.kt`
- Theme: `app/src/main/java/com/milkilabs/majra/ui/theme/*`
- Theme prefs: `app/src/main/java/com/milkilabs/majra/settings/*`
- Navigation doc: `docs/navigation.md`
- Theme doc: `docs/theme.md`
- Plugin doc: `docs/plugins.md`

Code style
- Kotlin style: `official`, 4-space indent, no tabs
- Imports: no wildcards; order stdlib, Android/Jetpack, third-party, project
- Formatting: trailing commas in multi-line args/data classes; one param per line
- Compose: prefer stateless composables; `Modifier` first optional param; keep recomposition cheap
- Naming: UpperCamelCase types, lowerCamelCase vars/functions, UPPER_SNAKE_CASE constants

Types & errors
- Prefer non-null types; avoid `!!`
- Use sealed classes/enums for closed sets
- Fail fast for programmer errors; surface recoverable issues clearly

Source plugins
- Source types use `SourceTypeId` (sealed) with Room converters in `data/db/Converters.kt`
- Register new sources via `SourcePlugin` + `SourcePluginRegistry` in `AppDependencies`
- UI type chips/menus are driven by plugin metadata (label + icon)

Comments
- Add brief comments only for non-obvious logic or edge cases

Working conventions
- Keep changes scoped to the task
- Avoid adding new tooling unless required
