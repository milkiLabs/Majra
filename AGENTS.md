# Agent Guide for Majra

This file is for coding agents working in this repo.
It summarizes build/test/lint commands and local code style.

Project type
- Android app (single module: :app)
- Kotlin + Jetpack Compose
- Gradle Kotlin DSL

Code style (Kotlin + Compose)
- Kotlin code style is `official` (see `gradle.properties`)
- Use 4-space indentation, no tabs
- Prefer explicit types for public APIs; infer for locals when obvious
- Keep functions small; favor pure functions where possible
- Avoid deep nesting; extract helpers instead

Imports
- Keep imports minimal and sorted
- No wildcard imports unless mandated by tooling
- Order: standard library, Android/Jetpack, third-party, project

Formatting
- Use trailing commas in multi-line argument lists and data classes
- One parameter per line for multi-line calls
- Place annotations on their own line above declarations
- Keep Compose modifiers on a single line when short; wrap when long

Naming conventions
- Packages: lower_snake_case
- Classes/objects: UpperCamelCase
- Functions/vars: lowerCamelCase
- Constants: UPPER_SNAKE_CASE
- Compose UI functions: nouns or noun phrases, `@Composable` in name not required
- Tests: descriptive names; prefer `shouldDoThing` or `whenX_thenY`

Types and nullability
- Favor non-null types; avoid `!!`
- Use `?` only when null is meaningful
- Prefer sealed classes or enums for closed sets
- Prefer immutable collections and `val`

Error handling
- Fail fast for programmer errors with `require`, `check`, or `error`
- For recoverable issues, return `Result` or sealed UI state
- Avoid swallowing exceptions; log or surface intent clearly

Compose guidelines
- Stateless composables when possible; lift state up
- Use `remember` for local state only
- Keep recomposition cheap; avoid expensive work in composables
- Prefer `Modifier` as the first optional parameter with a default
- Use `@Preview` for small, isolated UI pieces

Project structure
- App entry: `app/src/main/java/com/example/majra/MainActivity.kt`
- Theme: `app/src/main/java/com/example/majra/ui/theme/*`

Theming
- Theme preferences live in `app/src/main/java/com/example/majra/settings/*`
- Theme documentation: `docs/theme.md`
- Add new palettes by updating `Color.kt`, `AccentPalette`, and `AccentPalette.tokens()`

Comments
- Add comments for most functions,classes etc especially for non-obvious logic, data flow, or edge cases

Navigation
- Navigation overview: `docs/navigation.md`

Working with this repo
- Do not add new build tools without justification
- Keep changes scoped to the task
- Update this file if you introduce new build/lint/test tooling
