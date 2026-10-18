package com.medibox.domain

import com.medibox.domain.ocr.CandidateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApproxDateOcrTest {

    @Test
    fun `year month formats keep precision`() {
        listOf("2028-03", "2028/03", "2028.03", "2028年03月").forEach { input ->
            val dates = CandidateParser.parseApproxDateCandidates(input)
            assertTrue(dates.isNotEmpty())
            assertEquals(ApproxDate.Precision.YEAR_MONTH, dates.first().precision)
            assertEquals("2028-03", dates.first().toStorageString())
        }
    }

    @Test
    fun `full dates keep full precision`() {
        listOf("2028-03-15", "2028/03/15", "2028.03.15", "2028年03月15日").forEach { input ->
            val dates = CandidateParser.parseApproxDateCandidates(input)
            assertTrue(dates.isNotEmpty())
            assertEquals(ApproxDate.Precision.FULL_DATE, dates.first().precision)
            assertEquals("2028-03-15", dates.first().toStorageString())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `illegal dates are rejected`() {
        CandidateParser.parseApproxDateCandidates("2028-13-15")
    }
}
