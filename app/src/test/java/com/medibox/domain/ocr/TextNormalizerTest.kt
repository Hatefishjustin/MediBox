package com.medibox.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `normalize line replaces full width punctuation`() {
        assertEquals("生产日期:2028-03", TextNormalizer.normalizeLine("生产日期：2028－03"))
    }

    @Test
    fun `normalize lines removes blank lines`() {
        val lines = TextNormalizer.normalizeLines("\n  A  \n\nB\tC\n")
        assertEquals(listOf("A", "B C"), lines)
    }

    @Test
    fun `compact for matching removes spaces`() {
        assertEquals("批号LOT123", TextNormalizer.compactForMatching("批号 LOT 123"))
    }
}
