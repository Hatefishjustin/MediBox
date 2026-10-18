package com.medibox.data.local.db

import java.io.File

/**
 * 照片文件生命周期管理（与数据库解耦，便于测试时替换为假实现）。
 *
 * 规则（产品约束）：
 * - 照片存于 App 私有目录，数据库只保存路径；
 * - 删除药品时删除对应照片文件；
 * - 替换照片时删除旧文件；
 * - 照片文件不存在时由 UI 优雅降级（此处只负责删除）。
 */
interface PhotoFileManager {
    fun deleteIfExists(path: String?): Boolean
    fun deleteOldIfDifferent(oldPath: String?, newPath: String?)
}

class AppPhotoFileManager(private val photosDir: File) : PhotoFileManager {
    override fun deleteIfExists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { File(path).takeIf { it.exists() }?.delete() ?: false }
            .getOrDefault(false)
    }

    override fun deleteOldIfDifferent(oldPath: String?, newPath: String?) {
        if (oldPath != null && oldPath != newPath) deleteIfExists(oldPath)
    }
}
