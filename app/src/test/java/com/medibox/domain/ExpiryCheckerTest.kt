package com.medibox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpiryCheckerTest {

    private val now = LocalDate.of(2028, 3, 15)

    // ---- status ----

    @Test
    fun `null date returns UNKNOWN`() {
        assertEquals(ExpiryStatus.UNKNOWN, ExpiryChecker.status(null, now))
    }

    @Test
    fun `FULL_DATE before now returns EXPIRED`() {
        val date = ApproxDate.parse("2028-03-14")
        assertEquals(ExpiryStatus.EXPIRED, ExpiryChecker.status(date, now))
    }

    @Test
    fun `FULL_DATE same day returns EXPIRING_SOON`() {
        val date = ApproxDate.parse("2028-03-15")
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryChecker.status(date, now))
    }

    @Test
    fun `FULL_DATE within 30 days returns EXPIRING_SOON`() {
        val date = ApproxDate.parse("2028-04-10")
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryChecker.status(date, now))
    }

    @Test
    fun `FULL_DATE beyond 30 days returns NORMAL`() {
        val date = ApproxDate.parse("2028-05-01")
        assertEquals(ExpiryStatus.NORMAL, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH current month returns EXPIRING_SOON`() {
        // 2028-03 means end of March; now is March 15, so still within the expiry month window
        val date = ApproxDate.parse("2028-03")
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH next month beyond soon threshold returns NORMAL`() {
        // 2028-04 means end of April (Apr 30); now is Mar 15 → 46 days away, > 30-day threshold
        val date = ApproxDate.parse("2028-04")
        assertEquals(ExpiryStatus.NORMAL, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH within 30 days returns EXPIRING_SOON`() {
        val date = ApproxDate.parse("2028-03")
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH boundary day 2028-03-31 returns EXPIRING_SOON`() {
        val date = ApproxDate.parse("2028-03")
        val boundary = ExpiryChecker.daysUntilExpiry(date, LocalDate.of(2028, 3, 31))
        assertEquals(0L, boundary)
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryChecker.status(date, LocalDate.of(2028, 3, 31)))
    }

    @Test
    fun `YEAR_MONTH 2028-04-01 returns EXPIRED for March expiry`() {
        val date = ApproxDate.parse("2028-03")
        assertEquals(ExpiryStatus.EXPIRED, ExpiryChecker.status(date, LocalDate.of(2028, 4, 1)))
    }

    @Test
    fun `YEAR_MONTH far future returns NORMAL`() {
        val date = ApproxDate.parse("2029-01")
        assertEquals(ExpiryStatus.NORMAL, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH previous month returns EXPIRED`() {
        // 2028-02 means end of February; now is March 15, already expired
        val date = ApproxDate.parse("2028-02")
        assertEquals(ExpiryStatus.EXPIRED, ExpiryChecker.status(date, now))
    }

    @Test
    fun `YEAR_MONTH never treated as day 1`() {
        // 2028-03 should NOT be treated as 2028-03-01
        // now is March 15 → 2028-03 is still valid (expires end of March)
        val date = ApproxDate.parse("2028-03")
        val status = ExpiryChecker.status(date, now)
        assertEquals(ExpiryStatus.EXPIRING_SOON, status)
    }

    // ---- daysUntilExpiry ----

    @Test
    fun `daysUntilExpiry FULL_DATE`() {
        val date = ApproxDate.parse("2028-03-20")
        assertEquals(5L, ExpiryChecker.daysUntilExpiry(date, now))
    }

    @Test
    fun `daysUntilExpiry YEAR_MONTH uses end of month`() {
        val date = ApproxDate.parse("2028-03")
        // March 15 → March 31 = 16 days
        assertEquals(16L, ExpiryChecker.daysUntilExpiry(date, now))
    }

    @Test
    fun `daysUntilExpiry null returns null`() {
        assertNull(ExpiryChecker.daysUntilExpiry(null, now))
    }

    // ---- displayString ----

    @Test
    fun `displayString YEAR_MONTH`() {
        val date = ApproxDate.parse("2028-03")
        assertEquals("2028年3月", ExpiryChecker.displayString(date))
    }

    @Test
    fun `displayString FULL_DATE`() {
        val date = ApproxDate.parse("2028-03-15")
        assertEquals("2028年3月15日", ExpiryChecker.displayString(date))
    }

    @Test
    fun `displayString null returns null`() {
        assertNull(ExpiryChecker.displayString(null))
    }

    @Test
    fun `displayString never appends -01`() {
        val date = ApproxDate.parse("2028-03")
        val display = ExpiryChecker.displayString(date)
        assert(!display!!.contains("-01")) { "YEAR_MONTH 不应补 -01: $display" }
        assert(!display.contains("01日")) { "YEAR_MONTH 不应补日: $display" }
    }

    // ---- tagText ----

    @Test
    fun `tagText EXPIRED`() {
        assertEquals("已过期", ExpiryChecker.tagText(ExpiryStatus.EXPIRED))
    }

    @Test
    fun `tagText EXPIRING_SOON`() {
        assertEquals("临期", ExpiryChecker.tagText(ExpiryStatus.EXPIRING_SOON))
    }

    @Test
    fun `tagText NORMAL returns null`() {
        assertNull(ExpiryChecker.tagText(ExpiryStatus.NORMAL))
    }

    @Test
    fun `tagText UNKNOWN returns null`() {
        assertNull(ExpiryChecker.tagText(ExpiryStatus.UNKNOWN))
    }
}
