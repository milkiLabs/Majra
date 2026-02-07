package com.example.nav3recipes.deeplink.basic.ui

import com.example.nav3recipes.deeplink.basic.SearchKey

/**
 * String resources
 */
internal const val STRING_LITERAL_FILTER = "filter"
internal const val STRING_LITERAL_HOME = "home"
internal const val STRING_LITERAL_USERS = "users"
internal const val STRING_LITERAL_SEARCH = "search"
internal const val STRING_LITERAL_INCLUDE = "include"
internal const val PATH_BASE = "https://www.nav3recipes.com"
internal const val PATH_INCLUDE = "$STRING_LITERAL_USERS/$STRING_LITERAL_INCLUDE"
internal const val PATH_SEARCH = "$STRING_LITERAL_USERS/$STRING_LITERAL_SEARCH"
internal const val URL_HOME_EXACT = "$PATH_BASE/$STRING_LITERAL_HOME"

internal const val URL_USERS_WITH_FILTER = "$PATH_BASE/$PATH_INCLUDE/{$STRING_LITERAL_FILTER}"
internal val URL_SEARCH = "$PATH_BASE/$PATH_SEARCH" +
        "?${SearchKey::ageMin.name}={${SearchKey::ageMin.name}}" +
        "&${SearchKey::ageMax.name}={${SearchKey::ageMax.name}}" +
        "&${SearchKey::firstName.name}={${SearchKey::firstName.name}}" +
        "&${SearchKey::location.name}={${SearchKey::location.name}}"
