package com.example.majra.settings

/** UI-level theme modes that map to light/dark behavior. */
enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

/** Accent palettes that choose between curated color schemes. */
enum class AccentPalette(val label: String) {
    Evergreen("Evergreen"),
    Cedar("Cedar"),
    Oasis("Oasis"),
}

/** User-facing typography size options. */
enum class TypographyScale(val label: String) {
    Small("Small"),
    Default("Default"),
    Large("Large"),
}

/** Corner rounding density used to build Material shapes. */
enum class ShapeDensity(val label: String) {
    Rounded("Rounded"),
    Sharp("Sharp"),
}

/** Persisted theme preferences consumed by the app theme. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.System,
    val accentPalette: AccentPalette = AccentPalette.Evergreen,
    val typographyScale: TypographyScale = TypographyScale.Default,
    val shapeDensity: ShapeDensity = ShapeDensity.Rounded,
)
