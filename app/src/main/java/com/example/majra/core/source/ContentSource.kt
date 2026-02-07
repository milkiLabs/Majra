package com.example.majra.core.source

import com.example.majra.core.model.Article
import com.example.majra.core.model.Source

interface ContentSource {
    val type: String
    val displayName: String

    suspend fun fetchArticles(source: Source): List<Article>
}
