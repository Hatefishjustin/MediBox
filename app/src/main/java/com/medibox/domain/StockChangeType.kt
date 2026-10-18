package com.medibox.domain

/**
 * 库存变更类型。通过 Room TypeConverter 以枚举名（String）持久化。
 *
 * 方向约定（amount 永远为正整数，方向由类型决定）：
 * - PURCHASE  购入  → 增加
 * - INCREASE  增加  → 增加
 * - DECREASE  减少  → 减少
 * - USE       使用  → 减少
 * - DISCARD   丢弃  → 减少
 * - ADJUST    调整  → 直接设置最终库存（amount 即最终库存）
 */
enum class StockChangeType {
    PURCHASE,
    INCREASE,
    DECREASE,
    USE,
    DISCARD,
    ADJUST
}
