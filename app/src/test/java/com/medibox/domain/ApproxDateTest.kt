package com.medibox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 纯 JVM 单元测试（不依赖 Robolectric / Android），保证核心日期逻辑一定可运行。
 */
class ApproxDateTest {

    @Test
    fun yearMonth_parsesAndKeepsPrecision() {
        val d = ApproxDate.parse("2028-03")
        assertEquals(ApproxDate.Precision.YEAR_MONTH, d.precision)
        assertEquals(2028, d.year)
        assertEquals(3, d.month)
        assertNull(d.day)
        assertEquals("2028-03", d.toStorageString())
        assertEquals("2028-03", d.displayString())
    }

    @Test
    fun fullDate_parsesAndKeepsPrecision() {
        val d = ApproxDate.parse("2028-03-15")
        assertEquals(ApproxDate.Precision.FULL_DATE, d.precision)
        assertEquals(2028, d.year)
        assertEquals(3, d.month)
        assertEquals(15, d.day)
        assertEquals("2028-03-15", d.toStorageString())
    }

    @Test
    fun neverAppendsDay_one() {
        // 关键约束：2028-03 绝不被偷偷补成 2028-03-01。
        val d = ApproxDate.parse("2028-03")
        assertNotEquals("2028-03-01", d.toStorageString())
        assertNotEquals(ApproxDate.Precision.FULL_DATE, d.precision)
    }

    @Test
    fun rejectsBadFormat() {
        assertThrows(IllegalArgumentException::class.java) { ApproxDate.parse("2028/3") }
        assertThrows(IllegalArgumentException::class.java) { ApproxDate.parse("not-a-date") }
        assertThrows(IllegalArgumentException::class.java) { ApproxDate.parse("2028-13") }
    }

    @Test
    fun roundTrip_preservesBothPrecisions() {
        assertEquals("2028-03", ApproxDate.parse("2028-03").toStorageString())
        assertEquals("2028-03-15", ApproxDate.parse("2028-03-15").toStorageString())
    }
}
