package com.medibox.domain.ocr

/**
 * OCR 原始结果状态。
 * 只描述识别阶段，不涉及落库或医疗判断。
 */
enum class OcrStatus {
    PREPARING,
    ANALYZING,
    SUCCESS,
    FAILED,
}

/**
 * OCR 稳定性等级。
 * 不伪造 ML Kit 的字段级概率，只做粗粒度判断。
 */
enum class OcrConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
}

/**
 * 单条候选值。
 * value 仍然保持原始文本，不做事实确认。
 */
data class OcrFieldCandidate(
    val value: String,
    val sourceLine: String? = null,
    val confidenceLevel: OcrConfidenceLevel = OcrConfidenceLevel.MEDIUM,
)

/**
 * OCR 解析后的候选集合。
 * 同一字段可以保留多个候选，交给 S4 继续确认。
 */
data class OcrCandidate(
    val nameCandidates: List<OcrFieldCandidate> = emptyList(),
    val specificationCandidates: List<OcrFieldCandidate> = emptyList(),
    val manufacturerCandidates: List<OcrFieldCandidate> = emptyList(),
    val batchNoCandidates: List<OcrFieldCandidate> = emptyList(),
    val productionDateCandidates: List<OcrFieldCandidate> = emptyList(),
    val expiryDateCandidates: List<OcrFieldCandidate> = emptyList(),
    val sourceLines: List<String> = emptyList(),
) {
    val hasAnyCandidate: Boolean
        get() = nameCandidates.isNotEmpty() ||
            specificationCandidates.isNotEmpty() ||
            manufacturerCandidates.isNotEmpty() ||
            batchNoCandidates.isNotEmpty() ||
            productionDateCandidates.isNotEmpty() ||
            expiryDateCandidates.isNotEmpty()
}
