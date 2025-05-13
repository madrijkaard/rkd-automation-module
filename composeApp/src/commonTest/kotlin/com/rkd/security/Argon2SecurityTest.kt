package com.rkd.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.Base64

class Argon2SecurityTest {

    @Test
    fun `test encrypt and decrypt with valid inputs`() {

        val message = "SECRET_MESSAGE"
        val password = "MY_PASSWORD"

        val encryptedMessage = Argon2Security.encrypt(message, password)
        println("Encrypted: $encryptedMessage")

        assertNotEquals(message, encryptedMessage)

        val decryptedMessage = Argon2Security.decrypt(encryptedMessage, password)
        println("Decrypted: $decryptedMessage")

        assertEquals(message, decryptedMessage)
    }

    @Test
    fun `test encrypt with different salts produces different outputs`() {

        val message = "SECRET_MESSAGE"
        val password = "MY_PASSWORD"

        val encryptedMessage1 = Argon2Security.encrypt(message, password)
        val encryptedMessage2 = Argon2Security.encrypt(message, password)

        assertNotEquals(encryptedMessage1, encryptedMessage2)
    }

    @Test
    fun `test decrypt with incorrect key fails`() {

        val message = "SECRET_MESSAGE"
        val correctKey = "CORRECT_KEY"
        val wrongKey = "WRONG_KEY"

        val encryptedMessage = Argon2Security.encrypt(message, correctKey)

        try {
            Argon2Security.decrypt(encryptedMessage, wrongKey)
        } catch (e: Exception) {
            println("Decryption failed as expected with wrong key: ${e.message}")
            return
        }

        throw AssertionError("Decryption should fail with incorrect key")
    }

    @Test
    fun `test if salt and iv are concatenated in ciphertext`() {

        val message = "SECRET_MESSAGE+SALT+IV"
        val password = "MY_PASSWORD"

        val encryptedBase64 = Argon2Security.encrypt(message, password)

        val allBytes = Base64.getDecoder().decode(encryptedBase64)

        val saltSize = 16
        val ivSize = 16
        val minTotalSize = saltSize + ivSize + 1

        assert(allBytes.size >= minTotalSize) {
            "Tamanho esperado é pelo menos $minTotalSize bytes, mas obtivemos apenas ${allBytes.size}."
        }

        val salt = allBytes.copyOfRange(0, saltSize)
        val iv = allBytes.copyOfRange(saltSize, saltSize + ivSize)
        val cipherBytes = allBytes.copyOfRange(saltSize + ivSize, allBytes.size)

        assert(salt.isNotEmpty()) { "Salt não pode ser vazio." }
        assert(iv.isNotEmpty()) { "IV não pode ser vazio." }
        assert(cipherBytes.isNotEmpty()) { "Ciphertext não pode ser vazio." }

        println("Salt (16 bytes): ${salt.joinToString { "%02x".format(it) }}")
        println("IV   (16 bytes): ${iv.joinToString { "%02x".format(it) }}")
        println("Ciphertext (${cipherBytes.size} bytes): ${cipherBytes.joinToString { "%02x".format(it) }}")
    }

    @Test
    fun `test if IVs are different for two encryptions`() {

        val message = "SECRET_MESSAGE"
        val password = "MY_PASSWORD"

        val encryptedMessage1 = Argon2Security.encrypt(message, password)
        val encryptedMessage2 = Argon2Security.encrypt(message, password)

        val allBytes1 = Base64.getDecoder().decode(encryptedMessage1)
        val allBytes2 = Base64.getDecoder().decode(encryptedMessage2)

        val saltSize = 16
        val ivSize = 16

        val iv1 = allBytes1.copyOfRange(saltSize, saltSize + ivSize)
        val iv2 = allBytes2.copyOfRange(saltSize, saltSize + ivSize)

        assertNotEquals(iv1.joinToString { "%02x".format(it) }, iv2.joinToString { "%02x".format(it) }) {
            "Os IVs devem ser diferentes entre as criptografias."
        }

        println("IV1: ${iv1.joinToString { "%02x".format(it) }}")
        println("IV2: ${iv2.joinToString { "%02x".format(it) }}")
    }

    @Test
    fun `test decryption fails when IV is manipulated`() {

        val message = "SECRET_MESSAGE"
        val password = "MY_PASSWORD"

        val encryptedMessage = Argon2Security.encrypt(message, password)

        val allBytes = Base64.getDecoder().decode(encryptedMessage)

        val saltSize = 16
        val ivSize = 16

        val salt = allBytes.copyOfRange(0, saltSize)
        val iv = allBytes.copyOfRange(saltSize, saltSize + ivSize)
        val ciphertext = allBytes.copyOfRange(saltSize + ivSize, allBytes.size)

        iv[0] = (iv[0] + 1).toByte()

        val manipulatedBytes = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, manipulatedBytes, 0, salt.size)
        System.arraycopy(iv, 0, manipulatedBytes, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, manipulatedBytes, salt.size + iv.size, ciphertext.size)

        val manipulatedBase64 = Base64.getEncoder().encodeToString(manipulatedBytes)

        try {
            Argon2Security.decrypt(manipulatedBase64, password)
        } catch (e: Exception) {
            println("Decryption failed as expected with manipulated IV: ${e.message}")
            return
        }

        throw AssertionError("Decryption should fail when IV is manipulated")
    }

    @Test
    fun `test encrypt with empty password fails`() {
        val message = "SECRET_MESSAGE"
        val password = ""

        try {
            Argon2Security.encrypt(message, password)
        } catch (e: IllegalArgumentException) {
            println("Encryption failed as expected with empty password: ${e.message}")
            return
        }

        throw AssertionError("Encryption should fail with empty password")
    }

    @Test
    fun `test decrypt with empty password fails`() {
        val message = "SECRET_MESSAGE"
        val password = "MY_PASSWORD"
        val emptyPassword = ""

        val encryptedMessage = Argon2Security.encrypt(message, password)

        try {
            Argon2Security.decrypt(encryptedMessage, emptyPassword)
        } catch (e: IllegalArgumentException) {
            println("Decryption failed as expected with empty password: ${e.message}")
            return
        }

        throw AssertionError("Decryption should fail with empty password")
    }

    @Test
    fun `test encrypt and decrypt with large message`() {
        val message = "A".repeat(1_000_000)
        val password = "MY_PASSWORD"

        val encryptedMessage = Argon2Security.encrypt(message, password)
        println("Encrypted large message of size ${message.length}")

        val decryptedMessage = Argon2Security.decrypt(encryptedMessage, password)
        println("Decrypted large message of size ${decryptedMessage.length}")

        assertEquals(message, decryptedMessage) {
            "Decrypted message should match the original large message"
        }
    }

    @Test
    fun `test encrypt and decrypt with single character message`() {
        val message = "A"
        val password = "MY_PASSWORD"

        val encryptedMessage = Argon2Security.encrypt(message, password)
        println("Encrypted single character message: $encryptedMessage")

        val decryptedMessage = Argon2Security.decrypt(encryptedMessage, password)
        println("Decrypted single character message: $decryptedMessage")

        assertEquals(message, decryptedMessage) {
            "Decrypted message should match the original single character message"
        }
    }

    @Test
    fun `test encrypt and decrypt with very large password`() {
        val message = "SECRET_MESSAGE"
        val password = "P".repeat(10_000)

        val encryptedMessage = Argon2Security.encrypt(message, password)
        println("Encrypted message with large password")

        val decryptedMessage = Argon2Security.decrypt(encryptedMessage, password)
        println("Decrypted message with large password")

        assertEquals(message, decryptedMessage) {
            "Decrypted message should match the original message"
        }
    }
}