package com.medibox.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 有效期状态枚举。
 *
 * 医疗边界：只表达「信息管理」层面的事实（已过期/即将到期/正常/未知），
 * **绝不**表达任何医疗判断（建议服用/停止服用/立即处理/不要服用）。
 */
enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, NORMAL, UNKNOWN }

/**
 * 有效期判断（集中至 Domain 层，UI 不硬编码）。
 *
 * 精度安全比较规则：
 * - FULL_DATE "2028-03-15" → 与当天直接比较。
 * - YEAR_MONTH "2028-03"   → 当月最后一天为边界（月末才过期）。
 *   即 3 月 1 日时 "2028-03" 尚未过期；4 月 1 日起才算过期。
 *   绝不把 "2028-03" 当作 "2028-03-01" 处理。
 */
object ExpiryChecker {

    const val DEFAULT_SOON_DAYS = 30

    /** 判断有效期状态。 */
    fun status(
        expiryDate: ApproxDate?,
        now: LocalDate = LocalDate.now(),
        soonDays: Int = DEFAULT_SOON_DAYS
    ): ExpiryStatus {
        if (expiryDate == null) return ExpiryStatus.UNKNOWN
        val boundary = expiryBoundary(expiryDate) // 月末 or 当天
        val daysLeft = ChronoUnit.DAYS.between(now, boundary)
        return when {
            daysLeft < 0 -> ExpiryStatus.EXPIRED
            daysLeft <= soonDays.toLong() -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.NORMAL
        }
    }

    /**
     * 距过期剩余天数（可能为负 = 已过期）。
     * YEAR_MONTH 按月末计算。
     */
    fun daysUntilExpiry(
        expiryDate: ApproxDate?,
        now: LocalDate = LocalDate.now()
    ): Long? {
        if (expiryDate == null) return null
        val boundary = expiryBoundary(expiryDate)
        return ChronoUnit.DAYS.between(now, boundary)
    }

    /**
     * 展示串（保持精度，绝不补 -01）。
     * - YEAR_MONTH  → "2028年3月"
     * - FULL_DATE   → "2028年3月15日"
     */
    fun displayString(expiryDate: ApproxDate?): String? {
        if (expiryDate == null) return null
        return when (expiryDate.precision) {
            ApproxDate.Precision.YEAR_MONTH ->
                "${expiryDate.year}年${expiryDate.month}月"
            ApproxDate.Precision.FULL_DATE ->
                "${expiryDate.year}年${expiryDate.month}月${expiryDate.day}日"
        }
    }

    /**
     * 状态标签文字（用于 AppTag）。
     * NORMAL 和 UNKNOWN 不显示标签（中性，不打扰）。
     */
    fun tagText(status: ExpiryStatus): String? = when (status) {
        ExpiryStatus.EXPIRED -> "已过期"
        ExpiryStatus.EXPIRING_SOON -> "临期"
        ExpiryStatus.NORMAL -> null
        ExpiryStatus.UNKNOWN -> null
    }

    // ---- 内部 ----

    /** 计算过期边界日期：YEAR_MONTH 取月末，FULL_DATE 取当天。 */
    private fun expiryBoundary(date: ApproxDate): LocalDate = when (date.precision) {
        ApproxDate.Precision.YEAR_MONTH -> {
            val ym = YearMonth.of(date.year, date.month!!)
            ym.atEndOfMonth() // 月末
        }
        ApproxDate.Precision.FULL_DATE ->
            LocalDate.of(date.year, date.month!!, date.day!!)
    }
}
