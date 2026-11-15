package com.weighttracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun kgAlwaysShowsTwoDecimals() {
        assertEquals("76,85", kg(76.85))
        assertEquals("74,50", kg(74.5))
        assertEquals("80,00", kg(80.0))
    }

    @Test
    fun dec1ShowsOneDecimal() {
        assertEquals("27,1", dec1(27.1))
        assertEquals("6,4", dec1(6.4))
    }

    @Test
    fun kgSignedShowsSmallChanges() {
        assertEquals("−0,45", kgSigned(-0.45))
        assertEquals("+0,05", kgSigned(0.05))
        assertEquals("−0,30", kgSigned(-0.3))
        assertEquals("0,00", kgSigned(0.0))
    }

    @Test
    fun parseDecimalAcceptsComma() {
        assertEquals(76.85, parseDecimal("76,85")!!, 0.0)
    }
}
