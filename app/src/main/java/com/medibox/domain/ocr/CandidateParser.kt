package com.medibox.domain.ocr

import com.medibox.domain.ApproxDate

/**
 * 只做文本归类与候选提取，不做医疗推理。
 */
object CandidateParser {

    fun parse(rawText: String): OcrCandidate {
        val lines = TextNormalizer.normalizeLines(rawText)
        val nameCandidates = parseNameCandidates(lines)
        val specificationCandidates = parseSpecificationCandidates(lines)
        val manufacturerCandidates = parseManufacturerCandidates(lines)
        val batchNoCandidates = parseBatchNoCandidates(lines)
        val productionDateCandidates = parseProductionDateCandidates(lines)
        val expiryDateCandidates = parseExpiryDateCandidates(lines)
        return OcrCandidate(
            nameCandidates = nameCandidates,
            specificationCandidates = specificationCandidates,
            manufacturerCandidates = manufacturerCandidates,
            batchNoCandidates = batchNoCandidates,
            productionDateCandidates = productionDateCandidates,
            expiryDateCandidates = expiryDateCandidates,
            sourceLines = lines,
        )
    }

    fun parseApproxDateCandidates(line: String): List<ApproxDate> {
        val normalized = TextNormalizer.compactForMatching(line)
        val matches = mutableListOf<ApproxDate>()

        Regex("(20\\d{2})[-./](\\d{2})[-./](\\d{2})").find(normalized)?.let {
            val (y, m, d) = it.destructured
            matches += ApproxDate.parse("$y-$m-$d")
        }
        Regex("(20\\d{2})[-./](\\d{2})").find(normalized)?.let {
            val (y, m) = it.destructured
            matches += ApproxDate.parse("$y-$m")
        }
        Regex("(20\\d{2})年(\\d{2})月(\\d{2})日?").find(normalized)?.let {
            val (y, m, d) = it.destructured
            matches += ApproxDate.parse("$y-$m-$d")
        }
        Regex("(20\\d{2})年(\\d{2})月").find(normalized)?.let {
            val (y, m) = it.destructured
            matches += ApproxDate.parse("$y-$m")
        }
        return matches.distinctBy { it.toStorageString() }
    }

    private fun parseNameCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        lines.forEachIndexed { index, line ->
            if (looksLikeMedicineName(line)) {
                result += OcrFieldCandidate(
                    value = line,
                    sourceLine = line,
                    confidenceLevel = if (index == 0) OcrConfidenceLevel.HIGH else OcrConfidenceLevel.MEDIUM,
                )
            }
        }
        return dedupe(result)
    }

    private fun parseSpecificationCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        val specRegex = Regex("(?i)\\b\\d+(?:\\.\\d+)?(?:g|mg|ml|ug|μg|片|粒|支|袋|枚|盒)(?:[xX*×]\\d+(?:\\.\\d+)?(?:g|mg|ml|ug|μg|片|粒|支|袋|枚|盒|ml)?)?.*")
        val plainRegex = Regex("(?i)\\b\\d+(?:\\.\\d+)?(?:g|mg|ml|ug|μg)\\b")
        lines.forEach { line ->
            val matched = when {
                specRegex.containsMatchIn(line) -> line
                plainRegex.containsMatchIn(line) -> plainRegex.find(line)?.value
                else -> null
            }
            if (matched != null) {
                result += OcrFieldCandidate(matched.trim(), line, OcrConfidenceLevel.MEDIUM)
            }
        }
        return dedupe(result)
    }

    private fun parseManufacturerCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        val keywords = listOf("生产企业", "生产厂家", "企业名称", "制造商", "委托生产")
        lines.forEach { line ->
            val normalized = TextNormalizer.compactForMatching(line)
            val keyword = keywords.firstOrNull { normalized.contains(it) }
            if (keyword != null) {
                val value = line.substringAfter(keyword).substringAfter(":", line).trim().ifBlank { line }
                result += OcrFieldCandidate(value = value, sourceLine = line, confidenceLevel = OcrConfidenceLevel.HIGH)
            }
        }
        return dedupe(result)
    }

    private fun parseBatchNoCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        lines.forEach { line ->
            val normalized = TextNormalizer.compactForMatching(line)
            when {
                normalized.contains("批准文号") -> Unit
                normalized.contains("批号") || normalized.contains("批次") || normalized.contains("lotno") || normalized.contains("batchno") || normalized.contains("lot") -> {
                    val value = line.substringAfter(":", line).trim().ifBlank { line }
                    result += OcrFieldCandidate(value = value, sourceLine = line, confidenceLevel = OcrConfidenceLevel.HIGH)
                }
            }
        }
        return dedupe(result)
    }

    private fun parseProductionDateCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        lines.forEach { line ->
            val normalized = TextNormalizer.compactForMatching(line)
            if (normalized.contains("生产日期") || normalized.contains("mfg") || normalized.contains("制造日期")) {
                parseApproxDateCandidates(line).forEach { date ->
                    result += OcrFieldCandidate(date.toStorageString(), line, OcrConfidenceLevel.HIGH)
                }
            }
        }
        return dedupe(result)
    }

    private fun parseExpiryDateCandidates(lines: List<String>): List<OcrFieldCandidate> {
        val result = mutableListOf<OcrFieldCandidate>()
        lines.forEach { line ->
            val normalized = TextNormalizer.compactForMatching(line)
            if (normalized.contains("有效期") || normalized.contains("exp") || normalized.contains("截止日期")) {
                parseApproxDateCandidates(line).forEach { date ->
                    result += OcrFieldCandidate(date.toStorageString(), line, OcrConfidenceLevel.HIGH)
                }
            }
        }
        return dedupe(result)
    }

    private fun looksLikeMedicineName(line: String): Boolean {
        val normalized = TextNormalizer.compactForMatching(line)
        if (normalized.isBlank()) return false
        if (normalized.length < 2) return false
        if (normalized.contains("批准文号")) return false
        if (normalized.contains("生产企业") || normalized.contains("生产厂家") || normalized.contains("企业名称")) return false
        if (normalized.contains("有效期") || normalized.contains("批号") || normalized.contains("批次")) return false
        if (normalized.contains("http")) return false
        if (Regex("^\\d+$").matches(normalized)) return false
        if (Regex("^[A-Za-z0-9]+$").matches(normalized) && normalized.length < 4) return false
        return true
    }

    private fun dedupe(items: List<OcrFieldCandidate>): List<OcrFieldCandidate> {
        return items.distinctBy { it.value }
    }
}
