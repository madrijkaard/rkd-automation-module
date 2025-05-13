package com.rkd.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest.isEqual
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object Argon2Security {

    private val logger: Logger = LoggerFactory.getLogger(Argon2Security::class.java)

    private const val DEFAULT_KEY_LENGTH = 32
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16
    private const val MAC_SIZE = 32

    private data class EncryptedData(
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
        val mac: ByteArray
    ) {
        fun toByteArray(): ByteArray {

            val totalSize = salt.size + iv.size + ciphertext.size + mac.size
            val result = ByteArray(totalSize)

            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(iv, 0, result, salt.size, iv.size)
            System.arraycopy(ciphertext, 0, result, salt.size + iv.size, ciphertext.size)
            System.arraycopy(mac, 0, result, salt.size + iv.size + ciphertext.size, mac.size)

            return result
        }

        override fun equals(other: Any?): Boolean {

            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!salt.contentEquals(other.salt)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (!mac.contentEquals(other.mac)) return false

            return true
        }

        override fun hashCode(): Int {

            var result = salt.contentHashCode()

            result = 31 * result + iv.contentHashCode()
            result = 31 * result + ciphertext.contentHashCode()
            result = 31 * result + mac.contentHashCode()

            return result
        }

        companion object {

            fun fromByteArray(data: ByteArray, saltSize: Int, ivSize: Int, macSize: Int): EncryptedData {

                val salt = data.copyOfRange(0, saltSize)
                val iv = data.copyOfRange(saltSize, saltSize + ivSize)
                val ciphertext = data.copyOfRange(saltSize + ivSize, data.size - macSize)
                val mac = data.copyOfRange(data.size - macSize, data.size)

                return EncryptedData(salt, iv, ciphertext, mac)
            }
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, keyLength: Int = DEFAULT_KEY_LENGTH): ByteArray {

        logger.debug("Starting key derivation with Argon2.")

        require(password.isNotEmpty()) { "Password cannot be empty." }
        require(salt.size == SALT_SIZE) { "Salt must be $SALT_SIZE bytes long." }
        require(keyLength == 16 || keyLength == 32) { "Key length must be 16 (AES-128) or 32 (AES-256)." }

        return try {

            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withMemoryAsKB(65536)
                .withIterations(3)
                .withParallelism(1)
                .build()

            val generator = Argon2BytesGenerator()
            generator.init(params)

            val keyBytes = ByteArray(keyLength)
            generator.generateBytes(password.toByteArray(Charsets.UTF_8), keyBytes)

            logger.debug("Key derivation completed successfully.")
            keyBytes

        } catch (e: Exception) {
            logger.error("Error during key derivation: ${e.message}", e)
            throw SecurityException("Key derivation failed.", e)
        }
    }

    fun encrypt(message: String, password: String): String {

        require(message.isNotEmpty()) { "Message cannot be empty." }
        require(password.isNotEmpty()) { "Password cannot be empty." }

        return try {

            logger.debug("Starting encryption process.")

            val salt = Random.nextBytes(SALT_SIZE)
            val iv = Random.nextBytes(IV_SIZE)
            val derivedKey = deriveKey(password, salt)

            val ivSpec = IvParameterSpec(iv)
            val secretKey = SecretKeySpec(derivedKey, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            val ciphertext = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

            val macKey = derivedKey.copyOfRange(16, 32)
            val mac = generateHmac(salt + iv + ciphertext, macKey)

            val encryptedData = EncryptedData(salt, iv, ciphertext, mac)
            logger.debug("Encryption completed successfully.")

            Base64.getEncoder().encodeToString(encryptedData.toByteArray())
        } catch (e: Exception) {
            logger.error("Error during encryption: ${e.message}", e)
            throw SecurityException("Encryption failed.", e)
        }
    }

    fun decrypt(encryptedBase64: String, password: String): String {

        require(encryptedBase64.isNotEmpty()) { "Encrypted data cannot be empty." }
        require(password.isNotEmpty()) { "Password cannot be empty." }

        return try {

            logger.debug("Starting decryption process.")

            val allBytes = Base64.getDecoder().decode(encryptedBase64)
            val encryptedData = EncryptedData.fromByteArray(allBytes, SALT_SIZE, IV_SIZE, MAC_SIZE)

            val derivedKey = deriveKey(password, encryptedData.salt)
            val macKey = derivedKey.copyOfRange(16, 32)

            if (!verifyHmac(
                    encryptedData.salt + encryptedData.iv + encryptedData.ciphertext,
                    macKey,
                    encryptedData.mac
                )
            ) {
                logger.warn("HMAC verification failed during decryption.")
                throw SecurityException("Invalid HMAC")
            }

            val ivSpec = IvParameterSpec(encryptedData.iv)
            val secretKey = SecretKeySpec(derivedKey, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            logger.debug("Decryption completed successfully.")

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: SecurityException) {
            logger.error("Decryption security exception: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error during decryption: ${e.message}", e)
            throw SecurityException("Decryption failed.", e)
        }
    }

    private fun verifyHmac(data: ByteArray, key: ByteArray, hmacToVerify: ByteArray): Boolean {
        val expectedHmac = generateHmac(data, key)
        return isEqual(expectedHmac, hmacToVerify)
    }

    private fun generateHmac(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            mac.doFinal(data)
        } catch (e: Exception) {
            logger.error("Error generating HMAC: ${e.message}", e)
            throw SecurityException("HMAC generation failed.", e)
        }
    }
}
