package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildGreetingTest {

    @Test
    fun realNameIsUsedAsSupplied() {
        assertEquals("ليان", ChildGreeting.displayName("ليان"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("ليان", ChildGreeting.displayName("  ليان  "))
    }

    @Test
    fun missingNameFallsBackToTheNeutralPhrase() {
        assertNull(ChildGreeting.displayName(null))
    }

    @Test
    fun blankNameFallsBackToTheNeutralPhrase() {
        assertNull(ChildGreeting.displayName(""))
        assertNull(ChildGreeting.displayName("   "))
        assertNull(ChildGreeting.displayName("\n\t"))
    }

    @Test
    fun embeddedNewlinesCollapseIntoASingleLine() {
        assertEquals("ليان محمد", ChildGreeting.displayName("ليان\nمحمد"))
    }

    @Test
    fun overlongNameIsTruncatedForTheHeader() {
        val result = ChildGreeting.displayName("ا".repeat(40))

        assertEquals(ChildGreeting.MAX_DISPLAY_LENGTH + 1, result!!.length)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun nameAtTheLimitIsNotTruncated() {
        val name = "ا".repeat(ChildGreeting.MAX_DISPLAY_LENGTH)

        assertEquals(name, ChildGreeting.displayName(name))
    }
}
