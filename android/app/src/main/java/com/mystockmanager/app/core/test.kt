package com.mystockmanager.app.core

fun main() {
    val result = CryptoManager().hashPassword(
        "café1234",
        "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e"
    )
    println("Kotlin hash: $result")
}