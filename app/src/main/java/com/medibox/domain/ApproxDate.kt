package com.medibox.domain

import java.util.Locale

/**
 * 精度安全的日期。
 *
 * 支持两种精度：
 * - [Precision.YEAR_MONTH]  -> "2028-03"（用户只知道年月）
 * - [Precision.FULL_DATE]   -> "2028-03-15"（完整日）
 *
 * 关键约束（来自产品边界）：
 * - 绝不自动把 "2028-03" 补成 "2028-03-01"；
 * - 绝不把只知道月份的日期转换成完整日期。
 *
 * 持久化规范串零填充（YYYY-MM / YYYY-MM-DD），字典序即时间序，可直接做范围查询。
 */
data class ApproxDate(
    val precision: Precision,
    val year: Int,
    val month: Int? = null,
    val day: Int? = null
) {
    enum class Precision { YEAR_MONTH, FULL_DATE }

    /** 持久化用的规范串（保持用户录入精度，不补日）。 */
    fun toStorageString(): String = when (precision) {
        Precision.YEAR_MONTH ->
            String.format(Locale.US, "%04d-%02d", year, requireNotNull(month) { "YEAR_MONTH 必须包含 month" })
        Precision.FULL_DATE ->
            String.format(
                Locale.US, "%04d-%02d-%02d",
                year,
                requireNotNull(month) { "FULL_DATE 必须包含 month" },
                requireNotNull(day) { "FULL_DATE 必须包含 day" }
            )
    }

    /** 展示串（与存储精度一致，绝不偷偷补 -01）。 */
    fun displayString(): String = toStorageString()

    companion object {
        private val FULL = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        private val YEAR_MONTH = Regex("^\\d{4}-\\d{2}$")

        fun parse(value: String): ApproxDate = when {
            FULL.matches(value) -> {
                val (y, m, d) = value.split("-").map { it.toInt() }
                require(m in 1..12) { "月份必须在 1-12 范围内: $m" }
                require(d in 1..31) { "日期必须在 1-31 范围内: $d" }
                ApproxDate(Precision.FULL_DATE, y, m, d)
            }
            YEAR_MONTH.matches(value) -> {
                val (y, m) = value.split("-").map { it.toInt() }
                require(m in 1..12) { "月份必须在 1-12 范围内: $m" }
                ApproxDate(Precision.YEAR_MONTH, y, m)
            }
            else -> throw IllegalArgumentException(
                "不支持的日期格式: $value（仅支持 YYYY-MM 或 YYYY-MM-DD）"
            )
        }
    }
}
