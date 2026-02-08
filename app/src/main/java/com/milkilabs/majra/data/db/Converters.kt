package com.milkilabs.majra.data.db

import androidx.room.TypeConverter
import com.milkilabs.majra.core.model.SourceTypeId

class SourceTypeIdConverter {
    @TypeConverter
    fun fromSourceTypeId(type: SourceTypeId): String = type.value

    @TypeConverter
    fun toSourceTypeId(value: String): SourceTypeId = SourceTypeId.fromValue(value)
}
