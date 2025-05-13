package com.rkd.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class CompressionSecurityTest {

    @Test
    fun `test compression and decompression with 1 private key`() {

        val keyPairs = List(1) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }

        val compressed = CompressionSecurity.compress(privateKeys)
        val decompressed = CompressionSecurity.decompress(compressed)

        assertEquals(privateKeys, decompressed, "Decompressed keys should match the original keys")
    }

    @Test
    fun `test compression and decompression with 5 private keys`() {

        val keyPairs = List(5) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }

        val compressed = CompressionSecurity.compress(privateKeys)
        val decompressed = CompressionSecurity.decompress(compressed)

        assertEquals(privateKeys, decompressed, "Decompressed keys should match the original keys")
    }

    @Test
    fun `test compression and decompression with 10 private keys`() {

        val keyPairs = List(10) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }

        val compressed = CompressionSecurity.compress(privateKeys)
        val decompressed = CompressionSecurity.decompress(compressed)

        assertEquals(privateKeys, decompressed, "Decompressed keys should match the original keys")
    }

    @Test
    fun `test compression and decompression with 20 private keys`() {

        val keyPairs = List(20) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }

        val compressed = CompressionSecurity.compress(privateKeys)
        val decompressed = CompressionSecurity.decompress(compressed)

        assertEquals(privateKeys, decompressed, "Decompressed keys should match the original keys")
    }
}