package com.medibox.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medibox.domain.ApproxDate

/**
 * 药品（一条记录 = 一盒/一瓶具体药品，含批次维度）。
 *
 * 字段分两类：
 * - 事实数据（来自包装/用户录入的客观信息）：name / specification / manufacturer /
 *   batchNo / productionDate / expiryDate / locationId / stock / photoPath
 * - 用户备注类（程序绝不生成，只能由用户手动填写）：usageNote /
 *   contraindicationNote / precautionNote / userNote
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val specification: String? = null,
    val manufacturer: String? = null,
    @ColumnInfo(name = "batch_no") val batchNo: String? = null,
    @ColumnInfo(name = "production_date") val productionDate: ApproxDate? = null,
    @ColumnInfo(name = "expiry_date") val expiryDate: ApproxDate? = null,
    @ColumnInfo(name = "location_id") val locationId: Long? = null,
    val stock: Int = 1,
    @ColumnInfo(name = "photo_path") val photoPath: String? = null,
    // ---- 用户备注类：默认 null，OCR 不填，仅用户手填 ----
    @ColumnInfo(name = "usage_note") val usageNote: String? = null,
    @ColumnInfo(name = "contraindication_note") val contraindicationNote: String? = null,
    @ColumnInfo(name = "precaution_note") val precautionNote: String? = null,
    @ColumnInfo(name = "user_note") val userNote: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
