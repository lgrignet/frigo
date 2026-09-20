package com.mystockmanager.app.core

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val iterations = 150000
    private val keyLength = 256
    private val secureRandom = SecureRandom()

    fun generateSalt(bytes: Int = 32): String {
        val salt = ByteArray(bytes)
        secureRandom.nextBytes(salt)
        return toHex(salt)
    }

    fun hashPassword(password: String, saltHex: String): String {
        val salt = fromHex(saltHex)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateRecoveryCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = StringBuilder()
        for (i in 0 until 20) {
            if (i > 0 && i % 5 == 0) code.append("-")
            code.append(chars[secureRandom.nextInt(chars.length)])
        }
        return code.toString()
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun fromHex(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
