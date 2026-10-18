package com.medibox.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medibox.domain.StockChangeType

/**
 * 库存变更流水。
 *
 * - [amount] 永远为正整数；方向由 [changeType] 决定（见 [StockChangeType]）。
 * - [balance] 是变更后的库存**快照**，仅供历史展示，不是库存真值。
 *   库存唯一真值是 [Medicine.stock]。
 * - 删除药品时，本表记录随外键 CASCADE 一起删除。
 */
@Entity(
    tableName = "stock_records",
    foreignKeys = [ForeignKey(
        entity = Medicine::class,
        parentColumns = ["id"],
        childColumns = ["medicine_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicine_id")]
)
data class StockRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "medicine_id") val medicineId: Long,
    @ColumnInfo(name = "change_type") val changeType: StockChangeType,
    val amount: Int,
    val balance: Int,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
