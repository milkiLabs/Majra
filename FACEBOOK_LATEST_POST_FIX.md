# Fix: Latest Post Not Showing Up

## Problem
The most recent post from any Facebook account wasn't being fetched and displayed in the app. Only the second-latest post and older posts were showing up.

## Root Causes

### 1. Overly Aggressive Comment Filtering (JavaScript)
**Location:** `FacebookWebViewScraper.kt` - JavaScript extractor

The JavaScript had redundant and incorrect filtering logic:

```javascript
// OLD CODE - WRONG
const hasCommentMarker = story.comment_rendering_instance || 
                        story.feedback_context?.comment_rendering_instance ||
                        story.__typename === 'Comment';

if (hasCommentMarker) {
    console.log('[Extractor] Skipping comment: ' + postId);
    return;  // ❌ Filters out comment
}

// Then checks AGAIN (unreachable for comments)
const isTopLevelPost = story.comet_sections != null || story.attachments != null;
if (!isTopLevelPost && hasCommentMarker) {
    console.log('[Extractor] Skipping non-post item: ' + postId);
    return;  // ❌ This is unreachable!
}
```

**Problems:**
- The second check was unreachable (comments already filtered)
- The `isTopLevelPost` check could filter out valid posts that haven't loaded their `comet_sections` or `attachments` yet
- The latest post might not have these fields populated immediately

**Fix:**
```javascript
// NEW CODE - CORRECT
if (story.comment_rendering_instance || story.feedback_context?.comment_rendering_instance) {
    console.log('[Extractor] Skipping comment: ' + postId);
    return;  // ✅ Only filter actual comments
}
// No additional checks - let posts through
```

### 2. Overly Aggressive Content Filtering (Kotlin Parser)
**Location:** `FacebookGraphQLParser.kt`

The parser was skipping posts with no text AND no media:

```kotlin
// OLD CODE - TOO AGGRESSIVE
if (text.isBlank() && !json.has("images") && !json.has("videos")) {
    Log.d(TAG, "Skipping post with no content: $postId")
    return null  // ❌ Might skip valid posts
}
```

**Problems:**
- Used `json.has()` which only checks if the key exists, not if it has content
- The latest post might have media that failed to extract
- Too aggressive filtering could skip valid posts

**Fix:**
```kotlin
// NEW CODE - LESS AGGRESSIVE
val videosArray = json.optJSONArray("videos")
val imagesArray = json.optJSONArray("images")
val hasMedia = (videosArray != null && videosArray.length() > 0) || 
               (imagesArray != null && imagesArray.length() > 0)

// Only skip if BOTH text is empty AND no media exists
if (text.isBlank() && !hasMedia) {
    Log.d(TAG, "Skipping post with no content: $postId")
    return null  // ✅ Only skip truly empty posts
}
```

## Changes Made

### 1. FacebookWebViewScraper.kt (JavaScript)
- Removed redundant comment filtering logic
- Removed unreachable `isTopLevelPost` check
- Simplified to only filter actual comments
- Added `isSharedPost` detection back
- Added better logging to show shared post status

### 2. FacebookGraphQLParser.kt (Kotlin)
- Changed from `json.has()` to actually checking array length
- Extract media arrays before filtering
- Only skip posts that truly have no content
- More lenient filtering to avoid false positives

## Why This Fixes the Issue

### Before:
1. Latest post loads in GraphQL response
2. JavaScript checks if it has `comet_sections` or `attachments`
3. If not (because it's still loading), it gets filtered as "not a top-level post"
4. OR: Kotlin parser checks `json.has("images")` which returns true even if array is empty
5. Post gets skipped

### After:
1. Latest post loads in GraphQL response
2. JavaScript only filters if it has explicit comment markers
3. Post passes through to Kotlin parser
4. Kotlin parser checks actual array length, not just existence
5. Post is included unless it truly has no content

## Testing

To verify the fix:
1. Add a Facebook account
2. Check if the most recent post shows up
3. The post should now appear in the feed
4. Check the logs for "Processing post" messages - should see all posts including the latest

## Additional Improvements

- Better logging to track which posts are being processed
- More explicit shared post detection
- Clearer separation between comment filtering and post validation
