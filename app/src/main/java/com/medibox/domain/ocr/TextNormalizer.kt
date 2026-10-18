package com.medibox.domain.ocr

/**
 * OCR 文本归一化。
 * 只做文本层处理，不做领域判断。
 */
object TextNormalizer {

    fun normalizeLines(rawText: String): List<String> {
        return rawText
            .lineSequence()
            .map { normalizeLine(it) }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun normalizeLine(line: String): String {
        return line
            .replace('\u3000', ' ')
            .replace(Regex("[\t\r]+"), " ")
            .replace(Regex("\\s+"), " ")
            .replace('：', ':')
            .replace('－', '-')
            .replace('—', '-')
            .replace('／', '/')
            .replace('×', '*')
            .replace('＊', '*')
            .replace('（', '(')
            .replace('）', ')')
            .trim()
    }

    fun compactForMatching(line: String): String {
        return normalizeLine(line).replace(" ", "")
    }
}
