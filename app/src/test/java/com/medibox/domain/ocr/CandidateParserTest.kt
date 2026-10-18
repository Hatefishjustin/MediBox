package com.medibox.domain.ocr

import com.medibox.domain.ApproxDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateParserTest {

    @Test
    fun `parses name spec manufacturer batch and dates`() {
        val raw = """
            布洛芬缓释胶囊
            0.3g*20粒
            生产企业：华润三九医药股份有限公司
            批号：B20280315
            生产日期：2026-03-15
            有效期至 2028-03
        """.trimIndent()

        val candidate = CandidateParser.parse(raw)

        assertTrue(candidate.nameCandidates.any { it.value.contains("布洛芬") })
        assertTrue(candidate.specificationCandidates.any { it.value.contains("0.3g*20粒") })
        assertTrue(candidate.manufacturerCandidates.any { it.value.contains("华润三九") })
        assertTrue(candidate.batchNoCandidates.any { it.value.contains("B20280315") })
        assertTrue(candidate.productionDateCandidates.any { it.value == "2026-03-15" })
        assertTrue(candidate.expiryDateCandidates.any { it.value == "2028-03" })
    }

    @Test
    fun `prefers candidates and keeps multiple options`() {
        val raw = """
            维生素C片
            VC片
            生产企业：A药业有限公司
            生产企业：B药业有限公司
        """.trimIndent()

        val candidate = CandidateParser.parse(raw)

        assertTrue(candidate.nameCandidates.size >= 1)
        assertTrue(candidate.manufacturerCandidates.size >= 2)
    }

    @Test
    fun `ignores approval number as batch`() {
        val raw = """
            批准文号：国药准字H20234567
            批号：LOT-12345
        """.trimIndent()

        val candidate = CandidateParser.parse(raw)

        assertEquals(1, candidate.batchNoCandidates.size)
        assertEquals("LOT-12345", candidate.batchNoCandidates.first().value)
    }

    @Test
    fun `parseApproxDateCandidates keeps YEAR_MONTH precision`() {
        val dates = CandidateParser.parseApproxDateCandidates("有效期至 2028-03")
        assertEquals(1, dates.size)
        assertEquals(ApproxDate.Precision.YEAR_MONTH, dates.first().precision)
        assertEquals("2028-03", dates.first().toStorageString())
    }

    @Test
    fun `parseApproxDateCandidates accepts mixed separators`() {
        val values = listOf("2028/03", "2028.03", "2028年03月")
            .flatMap { CandidateParser.parseApproxDateCandidates(it) }
            .map { it.toStorageString() }

        assertTrue(values.contains("2028-03"))
    }

    @Test
    fun `empty text yields no candidates`() {
        val candidate = CandidateParser.parse("   \n  \t")
        assertTrue(candidate.sourceLines.isEmpty())
        assertTrue(!candidate.hasAnyCandidate)
    }
}
