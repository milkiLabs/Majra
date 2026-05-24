# yt-dlp Video Extraction Analysis for Majra

## Overview
This document analyzes yt-dlp's Facebook video extraction approach and provides recommendations for improving video playback in the Majra Android app.

## Current State in Majra

### What We Have
1. **WebView-based GraphQL Interception**: Captures Facebook's internal API responses
2. **JavaScript Injection**: Intercepts fetch/XHR calls to capture GraphQL data
3. **DOM Scraping**: Extracts video elements from rendered page
4. **Video Indicator Detection**: Identifies posts with videos even without playable URLs

### Current Limitations
- Video URLs are often blob URLs or require authentication
- Many videos show indicators but no playable URLs
- Limited format/quality selection
- No DASH manifest support
- Videos may not play outside Facebook's context

## yt-dlp's Approach

### Key Strategies

#### 1. **Multiple Video URL Fields**
yt-dlp checks numerous fields for video URLs (lines 636-900):

```python
# Legacy format fields
'playable_url'              # SD quality
'playable_url_quality_hd'   # HD quality
'browser_native_hd_url'     # HD native
'browser_native_sd_url'     # SD native
'playable_url_dash'         # DASH manifest

# New videoDeliveryResponse fields
'videoDeliveryResponseFragment'
'videoDeliveryResponseResult'
'progressive_urls'          # Progressive download URLs
'dash_manifest_urls'        # DASH manifests
'hls_playlist_urls'         # HLS playlists
```

#### 2. **DASH Manifest Extraction**
yt-dlp extracts and parses DASH manifests for adaptive streaming:

```python
def extract_dash_manifest(vid_data, formats, mpd_url=None):
    dash_manifest = traverse_obj(
        vid_data, 'dash_manifest', 'playlist', 
        'dash_manifest_xml_string', 'manifest_xml'
    )
    if dash_manifest:
        formats.extend(self._parse_mpd_formats(
            compat_etree_fromstring(urllib.parse.unquote_plus(dash_manifest)),
            mpd_url=url_or_none(vid_data.get('dash_manifest_url'))
        ))
```

#### 3. **Quality Prioritization**
Uses a quality ranking system:
- HD formats get higher priority (+1)
- DASH formats prioritized over progressive
- Formats without resolution info are deprioritized (-3)

#### 4. **User-Agent Spoofing**
Critical for avoiding rate limiting:

```python
f['http_headers']['User-Agent'] = 'facebookexternalhit/1.1'
```

#### 5. **Chunk Size Regulation**
Prevents 403 errors on large files:

```python
f['downloader_options']['http_chunk_size'] = 250 << 20  # 250MB chunks
```

#### 6. **Multiple Extraction Paths**
yt-dlp tries several data extraction methods in order:
1. GraphQL relay data (preferred)
2. Server JS data
3. BigPipe pagelets
4. Tahoe player endpoint (fallback)

#### 7. **Subtitle/Caption Support**
Extracts captions from multiple sources:
- `video_available_captions_locales`
- `captions_url`
- Distinguishes between automatic and manual captions

## Recommendations for Majra

### High Priority Improvements

#### 1. **Expand Video URL Field Search**
Update `GET_CAPTURED_GRAPHQL_SCRIPT` to check all yt-dlp video fields:

```javascript
const videoUrlFields = [
    // Legacy fields
    'playable_url',
    'playable_url_quality_hd',
    'browser_native_hd_url',
    'browser_native_sd_url',
    'playable_url_dash',
    
    // New delivery response fields
    'progressive_url',
    'download_url',
    'video_url',
    'source',
    'src',
    
    // Nested paths
    'videoDeliveryLegacyFields.playable_url',
    'videoDeliveryLegacyFields.playable_url_quality_hd',
    'videoDeliveryResponseFragment.videoDeliveryResponseResult.progressive_urls',
];
```

#### 2. **Add DASH Manifest Support**
Extract DASH manifests for adaptive streaming:

```javascript
// In deepSearchMedia function
const dashFields = [
    'dash_manifest',
    'dash_manifest_xml_string',
    'manifest_xml',
    'playlist'
];

for (let field of dashFields) {
    if (obj[field] && typeof obj[field] === 'string') {
        // Store DASH manifest for later parsing
        post.dashManifest = obj[field];
    }
}

// Check for dash_manifest_urls array
if (obj.dash_manifest_urls && Array.isArray(obj.dash_manifest_urls)) {
    for (let manifestObj of obj.dash_manifest_urls) {
        if (manifestObj.manifest_url) {
            post.dashManifestUrls = post.dashManifestUrls || [];
            post.dashManifestUrls.push(manifestObj.manifest_url);
        }
    }
}
```

#### 3. **Implement Quality Selection**
Store multiple quality options:

```kotlin
data class VideoFormat(
    val url: String,
    val quality: String,  // "sd", "hd", "dash"
    val formatId: String,
    val height: Int? = null,
    val isDash: Boolean = false
)

data class PostMediaItem(
    val imageUrl: String?,
    val videoUrl: String?,
    val videoFormats: List<VideoFormat> = emptyList(),  // NEW
    val dashManifestUrl: String? = null,                // NEW
    val mediaType: String
)
```

#### 4. **Add Fallback Video Loading**
For posts with video indicators but no URLs, implement a secondary loading mechanism:

```kotlin
suspend fun loadVideoUrl(postId: String, permalink: String): String? {
    // Use Facebook's video page template
    val videoPageUrl = "https://www.facebook.com/video/video.php?v=$postId"
    
    // Or use Tahoe endpoint (yt-dlp's fallback)
    val tahoeUrl = "https://www.facebook.com/video/tahoe/async/$postId/?chain=true&isvideo=true&payloadtype=primary"
    
    // Load in WebView and extract video URL
    return extractVideoFromPage(tahoeUrl)
}
```

#### 5. **Implement Custom User-Agent**
Add to video requests to avoid rate limiting:

```kotlin
private const val FB_VIDEO_USER_AGENT = "facebookexternalhit/1.1"

// When loading video URLs
val request = Request.Builder()
    .url(videoUrl)
    .header("User-Agent", FB_VIDEO_USER_AGENT)
    .build()
```

### Medium Priority Improvements

#### 6. **HLS Playlist Support**
Check for HLS playlists as alternative to DASH:

```javascript
// In deepSearchMedia
if (obj.hls_playlist_urls && Array.isArray(obj.hls_playlist_urls)) {
    for (let hlsObj of obj.hls_playlist_urls) {
        if (hlsObj.hls_playlist_url) {
            post.hlsPlaylistUrls = post.hlsPlaylistUrls || [];
            post.hlsPlaylistUrls.push(hlsObj.hls_playlist_url);
        }
    }
}
```

#### 7. **Video Metadata Extraction**
Capture additional video information:

```javascript
const videoMetadata = {
    duration: obj.playable_duration_in_ms || obj.length_in_second,
    width: obj.width || obj.original_width,
    height: obj.height || obj.original_height,
    thumbnailImage: obj.thumbnailImage?.uri || obj.preferred_thumbnail?.image?.uri,
    isLive: obj.is_video_broadcast || false,
    viewCount: obj.video_view_count
};
```

#### 8. **Subtitle/Caption Support**
Extract available captions:

```javascript
if (obj.video_available_captions_locales && Array.isArray(obj.video_available_captions_locales)) {
    post.captions = obj.video_available_captions_locales.map(caption => ({
        locale: caption.locale,
        url: caption.captions_url,
        language: caption.localized_language,
        isAutomatic: caption.localized_creation_method != null
    }));
}
```

### Low Priority / Future Enhancements

#### 9. **Watchparty Support**
Handle Facebook Watch Party videos (multiple videos in one post)

#### 10. **Live Video Detection**
Identify and handle live broadcasts differently:

```javascript
if (obj.is_video_broadcast || obj.__typename === 'LiveVideo') {
    post.isLive = true;
    post.liveStatus = obj.broadcast_status;
}
```

#### 11. **Video Thumbnail Quality**
Extract high-quality thumbnails:

```javascript
// yt-dlp checks multiple thumbnail sources
const thumbnailSources = [
    obj.thumbnailImage?.uri,
    obj.preferred_thumbnail?.image?.uri,
    obj.image?.uri,
    obj.picture
];
```

## Implementation Plan

### Phase 1: Quick Wins (1-2 days)
1. ✅ Expand video URL field search in JavaScript
2. ✅ Add quality metadata extraction
3. ✅ Implement custom User-Agent for video requests

### Phase 2: Core Improvements (3-5 days)
1. Add DASH manifest extraction and parsing
2. Implement multi-quality video format storage
3. Add fallback video loading for posts without URLs
4. Test with various Facebook video types

### Phase 3: Advanced Features (1 week)
1. HLS playlist support
2. Subtitle/caption extraction
3. Live video detection
4. Video metadata (duration, dimensions, view count)

### Phase 4: Polish (ongoing)
1. Handle edge cases (private videos, age-restricted, etc.)
2. Optimize video loading performance
3. Add video quality selection UI
4. Implement video caching strategy

## Code Changes Required

### 1. Update `FacebookWebViewScraper.kt`
- Expand `GET_CAPTURED_GRAPHQL_SCRIPT` with new video URL fields
- Add DASH manifest extraction
- Add HLS playlist extraction
- Extract video metadata

### 2. Update `FacebookGraphQLParser.kt`
- Parse multiple video formats
- Handle DASH manifest URLs
- Store quality options
- Parse video metadata

### 3. Update Data Models
- Add `VideoFormat` data class
- Update `PostMediaItem` with format list
- Add video metadata fields

### 4. Create Video Player Enhancement
- Support multiple quality selection
- Handle DASH/HLS adaptive streaming
- Implement fallback loading
- Add custom User-Agent

### 5. Add Video Loading Service
- Secondary video URL extraction
- Tahoe endpoint fallback
- Video URL caching

## Testing Strategy

### Test Cases
1. **Regular video posts** - Single video with SD/HD options
2. **Carousel with videos** - Multiple videos in one post
3. **Shared video posts** - Videos shared from other pages
4. **Live videos** - Active and archived live streams
5. **Private/restricted videos** - Videos requiring authentication
6. **Videos with captions** - Multiple language subtitles
7. **High-quality videos** - 1080p+ resolution
8. **Long videos** - 30+ minutes (chunk size testing)

### Success Metrics
- % of video posts with playable URLs (target: >90%)
- Video loading success rate (target: >95%)
- Average time to extract video URL (target: <3s)
- Quality options available (target: SD + HD for most videos)

## Security & Privacy Considerations

1. **Cookie Handling**: Ensure cookies are properly managed and not leaked
2. **User-Agent Spoofing**: Use responsibly, only for video requests
3. **Rate Limiting**: Implement delays between requests to avoid detection
4. **Terms of Service**: Ensure compliance with Facebook's ToS
5. **User Data**: Don't store or transmit video URLs to external servers

## References

- yt-dlp Facebook extractor: `/yt-dlp/yt_dlp/extractor/facebook.py`
- Current implementation: `FacebookWebViewScraper.kt`, `FacebookGraphQLParser.kt`
- Facebook GraphQL API: Internal, reverse-engineered
- DASH specification: ISO/IEC 23009-1
- HLS specification: RFC 8216

## Conclusion

yt-dlp's approach provides a robust framework for extracting Facebook videos. The key improvements for Majra are:

1. **Comprehensive field checking** - Look in all possible video URL locations
2. **DASH manifest support** - Enable adaptive streaming
3. **Quality selection** - Offer SD/HD options to users
4. **Fallback mechanisms** - Secondary loading for difficult videos
5. **Proper headers** - User-Agent and chunk size for reliability

By implementing these improvements in phases, Majra can significantly enhance its video playback capabilities while maintaining the existing GraphQL interception architecture.
