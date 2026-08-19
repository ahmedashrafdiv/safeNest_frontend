package com.example.safenest.util

object ParentRegistrationValidator {
    fun parentNameError(name: String): String? {
        return if (name.trim().length < 2) {
            "الرجاء إدخال اسم الأب الكامل"
        } else {
            null
        }
    }
}
