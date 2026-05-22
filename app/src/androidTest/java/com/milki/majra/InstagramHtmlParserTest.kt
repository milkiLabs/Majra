package com.milki.majra

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.milki.majra.data.scraper.InstagramHtmlParser
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class InstagramHtmlParserTest {
    @Test
    fun parseWebProfileInfoJson() {
        val json = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "username": "sample_user",
                  "full_name": "Sample User",
                  "profile_pic_url": "https://example.com/profile.jpg",
                  "edge_owner_to_timeline_media": {
                    "edges": [
                      {
                        "node": {
                          "id": "post-1",
                          "shortcode": "ABC123",
                          "display_url": "https://example.com/post.jpg",
                          "taken_at_timestamp": 1700000000,
                          "edge_media_to_caption": {
                            "edges": [
                              {
                                "node": {
                                  "text": "caption text"
                                }
                              }
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
              },
              "status": "ok"
            }
        """.trimIndent()

        val result = InstagramHtmlParser().parseProfile("sample_user", json)

        assertEquals("sample_user", result.account.username)
        assertEquals("Sample User", result.account.displayName)
        assertEquals("https://example.com/profile.jpg", result.account.profilePicUrl)
        assertEquals("123", result.userId)
        assertEquals(1, result.posts.size)
        assertEquals("ABC123", result.posts.first().shortcode)
        assertEquals("caption text", result.posts.first().caption)
    }

    @Test
    fun parseProfileJsonWithSeparateFeedItems() {
        val profileJson = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "username": "sample_user",
                  "full_name": "Sample User",
                  "profile_pic_url": "https://example.com/profile.jpg"
                }
              }
            }
        """.trimIndent()
        val feedJson = """
            {
                "items": [
                  {
                    "id": "post-1",
                    "code": "DEF456",
                    "taken_at": 1700000100,
                    "caption": {
                      "text": "feed caption"
                    },
                    "image_versions2": {
                      "candidates": [
                        {
                          "url": "https://example.com/feed.jpg"
                        }
                      ]
                    }
                  }
                ],
                "status": "ok"
            }
        """.trimIndent()

        val result = InstagramHtmlParser().parseProfile("sample_user", profileJson, feedJson)

        assertEquals("123", result.userId)
        assertEquals(1, result.posts.size)
        assertEquals("DEF456", result.posts.first().shortcode)
        assertEquals("https://example.com/feed.jpg", result.posts.first().imageUrl)
        assertEquals("feed caption", result.posts.first().caption)
    }

    @Test
    fun parseVideoFeedItem() {
        val profileJson = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "username": "sample_user",
                  "full_name": "Sample User",
                  "profile_pic_url": "https://example.com/profile.jpg"
                }
              }
            }
        """.trimIndent()
        val feedJson = """
            {
              "items": [
                {
                  "id": "video-1",
                  "code": "VID123",
                  "media_type": 2,
                  "taken_at": 1700000200,
                  "caption": {
                    "text": "video caption"
                  },
                  "image_versions2": {
                    "candidates": [
                      {
                        "url": "https://example.com/video-cover.jpg"
                      }
                    ]
                  },
                  "video_versions": [
                    {
                      "url": "https://example.com/video.mp4"
                    }
                  ]
                }
              ],
              "status": "ok"
            }
        """.trimIndent()

        val result = InstagramHtmlParser().parseProfile("sample_user", profileJson, feedJson)

        assertEquals(1, result.posts.size)
        assertEquals("video", result.posts.first().mediaType)
        assertEquals("https://example.com/video.mp4", result.posts.first().videoUrl)
        assertTrue(result.posts.first().isVideo)
    }

    @Test
    fun parseCarouselWithSidecar() {
        val profileJson = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "username": "sample_user",
                  "full_name": "Sample User",
                  "profile_pic_url": "https://example.com/profile.jpg"
                }
              }
            }
        """.trimIndent()
        val feedJson = """
            {
              "items": [
                {
                  "id": "carousel-1",
                  "code": "CAR123",
                  "taken_at": 1700000300,
                  "display_url": "https://example.com/cover.jpg",
                  "edge_sidecar_to_children": {
                    "edges": [
                      {
                        "node": {
                          "id": "c-item-1",
                          "display_url": "https://example.com/item1.jpg"
                        }
                      },
                      {
                        "node": {
                          "id": "c-item-2",
                          "display_url": "https://example.com/item2-cover.jpg",
                          "is_video": true,
                          "video_url": "https://example.com/item2.mp4"
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = InstagramHtmlParser().parseProfile("sample_user", profileJson, feedJson)

        assertEquals(1, result.posts.size)
        val post = result.posts.first()
        assertEquals("carousel", post.mediaType)
        assertEquals(2, post.mediaItems.size)
        assertEquals("https://example.com/item1.jpg", post.mediaItems[0].imageUrl)
        assertFalse(post.mediaItems[0].isVideo)
        assertEquals("https://example.com/item2-cover.jpg", post.mediaItems[1].imageUrl)
        assertEquals("https://example.com/item2.mp4", post.mediaItems[1].videoUrl)
        assertTrue(post.mediaItems[1].isVideo)
    }

    @Test
    fun parseCarouselWithCarouselMedia() {
        val profileJson = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "username": "sample_user",
                  "full_name": "Sample User",
                  "profile_pic_url": "https://example.com/profile.jpg"
                }
              }
            }
        """.trimIndent()
        val feedJson = """
            {
              "items": [
                {
                  "id": "carousel-2",
                  "code": "CAR456",
                  "taken_at": 1700000400,
                  "carousel_media": [
                    {
                      "id": "cm-item-1",
                      "image_versions2": {
                        "candidates": [{ "url": "https://example.com/cm1.jpg" }]
                      }
                    },
                    {
                      "id": "cm-item-2",
                      "media_type": 2,
                      "image_versions2": {
                        "candidates": [{ "url": "https://example.com/cm2-cover.jpg" }]
                      },
                      "video_versions": [{ "url": "https://example.com/cm2.mp4" }]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = InstagramHtmlParser().parseProfile("sample_user", profileJson, feedJson)

        assertEquals(1, result.posts.size)
        val post = result.posts.first()
        assertEquals("carousel", post.mediaType)
        assertEquals(2, post.mediaItems.size)
        assertEquals("https://example.com/cm1.jpg", post.mediaItems[0].imageUrl)
        assertFalse(post.mediaItems[0].isVideo)
        assertEquals("https://example.com/cm2-cover.jpg", post.mediaItems[1].imageUrl)
        assertEquals("https://example.com/cm2.mp4", post.mediaItems[1].videoUrl)
        assertTrue(post.mediaItems[1].isVideo)
    }

    @Test
    fun testParseProfile() {
        val file = File("/data/local/tmp/latest_profile.html")
        assertTrue("Could not find latest_profile.html at ${file.absolutePath}", file.exists())
        
        val html = file.readText()
        val parser = InstagramHtmlParser()
        try {
            val result = parser.parseProfile("muhammad61qa", html)
            println("PARSER_TEST: Parse success! Account: ${result.account.username}, posts count: ${result.posts.size}")
            println("PARSER_TEST: Display name: ${result.account.displayName}")
            println("PARSER_TEST: Profile pic: ${result.account.profilePicUrl}")
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Parsing failed with exception: ${e.message}")
        }
    }
}
