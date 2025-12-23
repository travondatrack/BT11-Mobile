package com.example.baitap11.utils

import java.security.MessageDigest

/**
 * Security Hardening:
 * Không bao giờ lưu password plain-text. Sử dụng SHA-256 để hash.
 * Trong thực tế nên dùng Argon2 hoặc PBKDF2 + Salt.
 */
object SecurityUtils {
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}