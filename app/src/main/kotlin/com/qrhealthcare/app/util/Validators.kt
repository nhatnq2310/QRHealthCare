package com.qrhealthcare.app.util

/**
 * Tiny validation helpers used by the auth and profile forms. They return
 * null on success, or a localised user-friendly error message on failure.
 * Keep messages short so they fit under TextField labels.
 */
object Validators {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    fun email(value: String): String? = when {
        value.isBlank() -> "Vui lòng nhập email"
        !EMAIL_REGEX.matches(value.trim()) -> "Email không hợp lệ"
        else -> null
    }

    fun password(value: String, minLen: Int = 6): String? = when {
        value.isEmpty() -> "Vui lòng nhập mật khẩu"
        value.length < minLen -> "Mật khẩu phải có ít nhất $minLen ký tự"
        else -> null
    }

    fun required(value: String, fieldName: String = "Trường này"): String? =
        if (value.isBlank()) "$fieldName không được để trống" else null
}
