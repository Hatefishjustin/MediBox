package com.medibox.domain.ocr

/**
 * 连续帧 OCR 稳定性跟踪。
 *
 * 目标：
 * - 节流之后再评估稳定性
 * - 相同 / 相似结果连续出现才视为稳定
 * - 单次错误 OCR 不应立刻打断主流程
 */
class StabilityTracker(
    private val requiredStableCount: Int = 3,
    private val maxHistorySize: Int = 5,
) {

    private val history = ArrayDeque<OcrCandidate>()

    fun reset() {
        history.clear()
    }

    fun push(candidate: OcrCandidate): StabilitySnapshot {
        history.addLast(candidate)
        while (history.size > maxHistorySize) {
            history.removeFirst()
        }
        return snapshot()
    }

    fun snapshot(): StabilitySnapshot {
        val latest = history.lastOrNull()
        val stableCount = countTailStable()
        return StabilitySnapshot(
            latestCandidate = latest,
            stableCount = stableCount,
            isStable = latest != null && stableCount >= requiredStableCount,
        )
    }

    private fun countTailStable(): Int {
        if (history.isEmpty()) return 0
        val latest = history.last()
        var count = 1
        val tail = history.toList().asReversed().drop(1)
        for (candidate in tail) {
            if (isSimilar(candidate, latest)) {
                count += 1
            } else {
                break
            }
        }
        return count
    }

    private fun isSimilar(first: OcrCandidate, second: OcrCandidate): Boolean {
        return score(first.nameCandidates, second.nameCandidates) >= 0.75 &&
            score(first.specificationCandidates, second.specificationCandidates) >= 0.5 &&
            score(first.manufacturerCandidates, second.manufacturerCandidates) >= 0.5 &&
            score(first.batchNoCandidates, second.batchNoCandidates) >= 0.5 &&
            score(first.productionDateCandidates, second.productionDateCandidates) >= 0.5 &&
            score(first.expiryDateCandidates, second.expiryDateCandidates) >= 0.5
    }

    private fun score(first: List<OcrFieldCandidate>, second: List<OcrFieldCandidate>): Double {
        if (first.isEmpty() && second.isEmpty()) return 1.0
        if (first.isEmpty() || second.isEmpty()) return 0.0
        val firstValues = first.map { it.value.lowercase() }.toSet()
        val secondValues = second.map { it.value.lowercase() }.toSet()
        val intersection = firstValues.intersect(secondValues)
        val union = firstValues.union(secondValues)
        return intersection.size.toDouble() / union.size.coerceAtLeast(1)
    }
}

data class StabilitySnapshot(
    val latestCandidate: OcrCandidate?,
    val stableCount: Int,
    val isStable: Boolean,
)
