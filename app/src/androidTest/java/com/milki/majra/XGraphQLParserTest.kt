package com.milki.majra

import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.platform.x.XGraphQLParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XGraphQLParserTest {
    @Test
    fun parseExtractsImagesVideosAndSortsNewestFirst() {
        val payload = """
            {
              "displayName": "Majra",
              "profilePicUrl": "https://pbs.twimg.com/profile_images/avatar.jpg",
              "posts": [
                {
                  "id": "100",
                  "text": "older image",
                  "timestamp": 1000,
                  "permalink": "https://x.com/majra/status/100",
                  "images": ["https://pbs.twimg.com/media/one.jpg"],
                  "videos": []
                },
                {
                  "id": "200",
                  "text": "newer video",
                  "timestamp": 2000,
                  "permalink": "https://x.com/majra/status/200",
                  "images": ["https://pbs.twimg.com/media/thumb.jpg"],
                  "videos": ["https://video.twimg.com/ext_tw_video/video.mp4"]
                }
              ],
              "debug": {"capturedCount": 2, "domPosts": 2}
            }
        """.trimIndent()

        val parsed = XGraphQLParser().parse("@Majra", payload)

        assertEquals(Platform.X, parsed.account.platform)
        assertEquals("majra", parsed.account.username)
        assertEquals("Majra", parsed.account.displayName)
        assertEquals(2, parsed.posts.size)
        assertEquals("200", parsed.posts.first().id)
        assertEquals(SocialPost.MEDIA_TYPE_VIDEO, parsed.posts.first().mediaType)
        assertTrue(parsed.posts.first().mediaItems.first().isVideo)
        assertEquals(PostMediaItem.MEDIA_TYPE_IMAGE, parsed.posts.last().mediaItems.first().mediaType)
    }
}
