package com.milki.majra.data.db

import androidx.room.TypeConverter
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import org.json.JSONArray
import org.json.JSONObject

class PostMediaConverters {
    @TypeConverter
    fun fromPlatform(platform: Platform?): String? = platform?.storageKey

    @TypeConverter
    fun toPlatform(value: String?): Platform? =
        Platform.entries.firstOrNull { it.storageKey == value } ?: value?.let { Platform.valueOf(it) }

    @TypeConverter
    fun fromMediaItemsList(list: List<PostMediaItem>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("imageUrl", item.imageUrl)
            obj.put("videoUrl", item.videoUrl ?: JSONObject.NULL)
            obj.put("mediaType", item.mediaType)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toMediaItemsList(value: String?): List<PostMediaItem> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<PostMediaItem>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val imageUrl = obj.getString("imageUrl")
                val videoUrl = if (obj.isNull("videoUrl")) null else obj.optString("videoUrl").takeIf { it.isNotBlank() && it != "null" }
                val mediaType = obj.optString("mediaType", PostMediaItem.MEDIA_TYPE_IMAGE)
                list.add(PostMediaItem(imageUrl, videoUrl, mediaType))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
