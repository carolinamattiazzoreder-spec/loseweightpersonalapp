package com.weighttracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun kgShowsUpToTwoDecimals() {
        assertEquals("76,85", kg(76.85))
        assertEquals("78,4", kg(78.4))
        assertEquals("80,0", kg(80.0))
    }

    @Test
    fun kgSignedShowsSmallChanges() {
        assertEquals("−0,45", kgSigned(-0.45))
        assertEquals("+0,05", kgSigned(0.05))
        assertEquals("0,0", kgSigned(0.0))
    }

    @Test
    fun parseDecimalAcceptsComma() {
        assertEquals(76.85, parseDecimal("76,85")!!, 0.0)
    }
}
