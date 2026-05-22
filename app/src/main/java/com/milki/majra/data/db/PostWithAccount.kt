package com.milki.majra.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.milki.majra.data.model.FeedItem

data class PostWithAccount(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "sourceKey",
        entityColumn = "sourceKey",
    )
    val account: AccountEntity,
) {
    fun toModel(): FeedItem = FeedItem(
        post = post.toModel(),
        account = account.toModel(),
    )
}
