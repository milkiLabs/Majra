# Facebook Post & Media Extraction Improvements

## Overview

After successfully fixing video playback by studying yt-dlp's approach, I've now applied the same principles to improve the overall post and media extraction. The new approach is more structured, reliable, and follows Facebook's actual GraphQL data structure.

## Key Problems with Old Approach

### 1. **Unstructured Deep Search**
- Used recursive `deepSearchMedia()` that blindly searched all objects
- No understanding of Facebook's attachment structure
- Mixed concerns: detection, extraction, and filtering all in one function
- Could miss media or extract wrong items

### 2. **Poor Image Quality**
- Grabbed any `uri` field without checking if it's the best quality
- Didn't prioritize high-resolution images
- No distinction between Photo objects and other image types

### 3. **Fragile Media Detection**
- Relied on finding specific field names anywhere in the tree
- Didn't follow Facebook's structured attachment patterns
- Could break when Facebook changes their structure

## New Approach (Based on yt-dlp)

### 1. **Structured Attachment Parsing**

Following yt-dlp's pattern, we now parse attachments in a structured way:

```javascript
function parseAttachments(attachments) {
    for (let attachment of attachments) {
        // Check for styles/style_type_renderer pattern (yt-dlp approach)
        const media = attachment.styles?.attachment?.media 
                   || attachment.style_type_renderer?.attachment?.media
                   || attachment.attachment_target_renderer?.attachment?.media
                   || attachment.throwbackStyles?.attachment_target_renderer?.attachment?.media
                   || attachment.media;
        
        if (media) {
            extractMedia(media);
        }
        
        // Check for all_subattachments (carousel/album posts)
        const subattachments = attachment.all_subattachments?.nodes 
                            || attachment.target?.attachments;
        if (subattachments) {
            for (let sub of subattachments) {
                const subMedia = sub.styles?.attachment?.media || sub.media;
                if (subMedia) {
                    extractMedia(subMedia);
                }
            }
        }
    }
}
```

**Benefits:**
- ✅ Follows Facebook's actual data structure
- ✅ Handles carousel/album posts correctly
- ✅ Supports multiple attachment patterns
- ✅ More maintainable and debuggable

### 2. **Type-Based Media Extraction**

Instead of searching for any field, we now check `__typename` first:

```javascript
function extractMedia(media) {
    const typename = media.__typename;
    
    // Handle Video objects
    if (typename === 'Video' || media.is_video_broadcast !== undefined) {
        // Extract video with quality priority
        const legacyFields = media.videoDeliveryLegacyFields || media;
        let videoUrl = legacyFields.playable_url_quality_hd  // HD first
                    || legacyFields.browser_native_hd_url
                    || legacyFields.playable_url              // SD fallback
                    || legacyFields.browser_native_sd_url;
        // ... extract video
    }
    // Handle Photo objects
    else if (typename === 'Photo') {
        // Extract highest quality image
        let imageUrl = media.viewer_image?.uri           // Best quality
                    || media.progressive_image?.uri      // Progressive load
                    || media.image?.uri                  // Standard
                    || media.photo_image?.uri;           // Fallback
        // ... extract photo
    }
}
```

**Benefits:**
- ✅ Type-safe extraction
- ✅ Prioritizes best quality
- ✅ Clear separation between video and photo handling
- ✅ Easier to add new media types

### 3. **Quality Prioritization**

#### For Videos:
1. `playable_url_quality_hd` - HD quality (preferred)
2. `browser_native_hd_url` - Browser-native HD
3. `playable_url` - SD quality
4. `browser_native_sd_url` - Browser-native SD
5. Progressive URLs from `videoDeliveryResponseFragment`

#### For Images:
1. `viewer_image.uri` - Full viewer quality
2. `progressive_image.uri` - Progressive load quality
3. `image.uri` - Standard quality
4. `photo_image.uri` - Fallback quality

### 4. **Better Filtering**

```javascript
// Filter out tiny images (profile pics, icons)
const width = media.viewer_image?.width || media.image?.width || 0;
const height = media.viewer_image?.height || media.image?.height || 0;
const isTiny = (width > 0 && height > 0 && (width < 100 || height < 100));

if (!isTiny && !images.includes(imageUrl)) {
    images.push(imageUrl);
}
```

**Benefits:**
- ✅ Excludes profile pictures and icons
- ✅ Only extracts actual post media
- ✅ Prevents duplicate images

## Architecture Comparison

### Old Approach (Unstructured)
```
Story → deepSearchMedia(everything) → find any uri/playable_url
```
- Searches entire object tree
- No structure awareness
- Mixed concerns

### New Approach (Structured)
```
Story → attachments → parseAttachments() → extractMedia(typed)
     ↓
     → comet_sections.content.story.attachments → parseAttachments()
     ↓
     → attached_story.attachments → parseAttachments()
```
- Follows Facebook's structure
- Type-aware extraction
- Separated concerns

## What This Fixes

### 1. **Missing Media**
- **Before**: Could miss media in subattachments or nested structures
- **After**: Explicitly handles `all_subattachments.nodes` for carousels

### 2. **Low Quality Images**
- **Before**: Grabbed first `uri` found, could be thumbnail
- **After**: Prioritizes `viewer_image` and `progressive_image` for best quality

### 3. **Wrong Media Extracted**
- **Before**: Could extract profile pics, icons, or UI elements
- **After**: Filters by size and checks `__typename`

### 4. **Carousel/Album Posts**
- **Before**: Might only get first image
- **After**: Properly iterates through `all_subattachments.nodes`

### 5. **Shared Posts**
- **Before**: Might extract media from wrong post
- **After**: Separately parses `attached_story` attachments

## Code Quality Improvements

### Separation of Concerns
- `parseAttachments()` - Handles attachment structure
- `extractMedia()` - Handles media type detection and extraction
- Clear, single-purpose functions

### Maintainability
- Easy to add new media types (just add another `else if (typename === 'NewType')`)
- Easy to adjust quality priorities
- Clear logging for debugging

### Reliability
- Follows Facebook's actual structure
- Less likely to break with Facebook changes
- Type-based detection is more stable than field name searching

## Testing Checklist

To verify the improvements:

1. **Single Image Post** - Should extract high-quality image
2. **Single Video Post** - Should extract HD video + thumbnail
3. **Carousel/Album Post** - Should extract all images/videos
4. **Shared Post** - Should extract media from shared content
5. **Text-Only Post** - Should handle gracefully (no media)
6. **Mixed Carousel** - Should handle images + videos in same post

## Performance Impact

- **Slightly faster**: No deep recursive search through entire object tree
- **More predictable**: Structured parsing has consistent performance
- **Better memory**: Doesn't traverse unnecessary parts of the tree

## Future Enhancements

Based on yt-dlp, we could add:

1. **DASH Manifest Support** - For adaptive streaming
2. **Multiple Quality Options** - Let users choose quality
3. **Subtitle Extraction** - For videos with captions
4. **Live Video Support** - Handle `is_video_broadcast` properly
5. **Story/Reel Support** - Extract from `short_form_video_context`

## References

- yt-dlp Facebook extractor: `/yt-dlp/yt_dlp/extractor/facebook.py`
- Key patterns:
  - `traverse_obj()` for safe nested access
  - `parse_attachment()` for structured parsing
  - Type-based media detection with `__typename`
  - Quality prioritization for formats
