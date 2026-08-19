package com.example.safenest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParentRegistrationValidatorTest {

    @Test
    fun test_blank_or_single_character_parent_name_blocks_registration() {
        listOf("", "   ", "أ").forEach { name ->
            assertEquals("الرجاء إدخال اسم الأب الكامل", ParentRegistrationValidator.parentNameError(name))
        }
    }

    @Test
    fun test_complete_parent_name_allows_registration() {
        assertNull(ParentRegistrationValidator.parentNameError("أحمد محمد"))
    }
}
