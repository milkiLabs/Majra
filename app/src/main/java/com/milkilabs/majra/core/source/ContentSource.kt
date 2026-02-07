package com.milkilabs.majra.core.source

import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.model.Source

interface ContentSource {
    val type: String
    val displayName: String

    suspend fun fetchArticles(source: Source): List<Article>
}
