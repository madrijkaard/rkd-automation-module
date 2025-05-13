package com.rkd.security

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object FileSecurity {

    private val logger = LoggerFactory.getLogger(FileSecurity::class.java)
    private const val ENCRYPTED_EXTENSION = ".encrypted"

    fun encrypt(filePath: String, password: String): String {

        val file = File(filePath)
        require(file.exists()) { "The file $filePath does not exist." }

        val encryptedFilePath = "$filePath$ENCRYPTED_EXTENSION"
        val encryptedFile = File(encryptedFilePath)

        try {

            val fileBytes: ByteArray
            file.inputStream().use { fis ->
                fileBytes = fis.readBytes()
            }

            val encryptedData = Argon2Security.encrypt(String(fileBytes, Charsets.UTF_8), password)
            encryptedFile.writeText(encryptedData)

            if (file.delete()) {
                logger.info("Original file $filePath was deleted after encryption.")
            } else {
                logger.warn("Failed to delete original file $filePath after encryption.")
                file.deleteOnExit()
                forceDeleteWithCmd(file.absolutePath)
            }
        } catch (e: Exception) {
            logger.error("Error during file encryption: ${e.message}", e)
            throw SecurityException("File encryption failed.", e)
        }

        logger.info("File $filePath was encrypted to $encryptedFilePath.")
        return encryptedFilePath
    }

    fun decrypt(filePath: String, password: String): String {

        val encryptedFile = File(filePath)
        require(encryptedFile.exists()) { "The file $filePath does not exist." }

        val decryptedFilePath = filePath.removeSuffix(ENCRYPTED_EXTENSION)
        val decryptedFile = File(decryptedFilePath)

        try {

            val encryptedData = encryptedFile.readText()
            val decryptedData = Argon2Security.decrypt(encryptedData, password)

            decryptedFile.writeText(decryptedData)

            if (encryptedFile.delete()) {
                logger.info("Encrypted file $filePath was deleted after decryption.")
            } else {
                logger.warn("Failed to delete encrypted file $filePath after decryption.")
            }

        } catch (e: Exception) {
            logger.error("Error during file decryption: ${e.message}", e)
            throw SecurityException("File decryption failed.", e)
        }

        logger.info("File $filePath was decrypted to $decryptedFilePath.")
        return decryptedFilePath
    }

    fun forceDeleteWithCmd(filePath: String) {
        try {
            val process = ProcessBuilder("cmd.exe", "/c", "del /f /q \"$filePath\"")
                .inheritIO()
                .start()
            process.waitFor()
            logger.info("File $filePath was deleted via Windows command.")
        } catch (e: IOException) {
            logger.error("Error during forced deletion of $filePath: ${e.message}", e)
        }
    }
}
