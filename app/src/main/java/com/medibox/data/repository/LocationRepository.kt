package com.medibox.data.repository

import com.medibox.data.local.db.dao.LocationDao
import com.medibox.data.local.db.entity.Location
import kotlinx.coroutines.flow.Flow

/**
 * 位置（药箱）仓库。
 *
 * 默认 Location「我的药箱」规则：
 * - 首次启动若无默认则创建一次；
 * - 后续启动若默认已存在则 no-op；
 * - 默认位置不可被删除（即使自定义位置全被删，默认仍在）。
 */
class LocationRepository(private val dao: LocationDao) {
    fun observeLocations(): Flow<List<Location>> = dao.observeAll()

    suspend fun getLocation(id: Long): Location? = dao.getById(id)

    suspend fun insert(name: String, isDefault: Boolean = false, sortOrder: Int = 0): Long =
        dao.insert(Location(name = name, isDefault = isDefault, sortOrder = sortOrder))

    suspend fun update(location: Location) = dao.update(location)

    /**
     * 删除位置：
     * - 默认位置（is_default）禁止删除；
     * - 仍有药品归属的位置禁止删除（避免孤儿数据）。
     */
    suspend fun delete(location: Location) {
        if (location.isDefault) throw IllegalStateException("默认位置「我的药箱」不可删除")
        if (dao.medicineCountForLocation(location.id) > 0)
            throw IllegalStateException("该位置仍有药品，无法删除（请先移走或删除其中的药品）")
        dao.delete(location)
    }

    suspend fun getDefault(): Location? = dao.getDefault()

    /**
     * 确保默认 Location 存在（幂等，只创建一次）。
     */
    suspend fun ensureDefaultLocation(defaultName: String = "我的药箱") {
        if (dao.getDefaultCount() == 0) {
            dao.insert(Location(name = defaultName, isDefault = true, sortOrder = 0))
        }
    }
}
