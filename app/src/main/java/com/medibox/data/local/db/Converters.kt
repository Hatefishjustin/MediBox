package com.medibox.data.local.db

import androidx.room.TypeConverter
import com.medibox.domain.ApproxDate
import com.medibox.domain.StockChangeType

/**
 * Room 类型转换。
 * - [ApproxDate] 以规范串（YYYY-MM / YYYY-MM-DD）持久化，保持精度，绝不补 -01。
 * - [StockChangeType] 以枚举名持久化。
 */
class Converters {
    @TypeConverter
    fun approxDateToStorage(value: ApproxDate?): String? = value?.toStorageString()

    @TypeConverter
    fun storageToApproxDate(value: String?): ApproxDate? = value?.let { ApproxDate.parse(it) }

    @TypeConverter
    fun stockChangeTypeToStorage(value: StockChangeType): String = value.name

    @TypeConverter
    fun storageToStockChangeType(value: String): StockChangeType = StockChangeType.valueOf(value)
}
