package com.rkd.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionSecurity {

    fun compress(privateKeys: List<BigInteger>): String {

        val concatenatedKeys = privateKeys.joinToString(separator = ",") { it.toString() }
        val byteArrayOutputStream = ByteArrayOutputStream()

        GZIPOutputStream(byteArrayOutputStream).use { gzip ->
            gzip.write(concatenatedKeys.toByteArray(Charsets.UTF_8))
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    fun decompress(compressedKeys: String): List<BigInteger> {

        val compressedBytes = Base64.getDecoder().decode(compressedKeys)
        val decompressedString = GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { gzip ->
            gzip.bufferedReader(Charsets.UTF_8).readText()
        }

        return decompressedString.split(",").map { BigInteger(it) }
    }
}