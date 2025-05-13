package com.rkd.security

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security.addProvider

private const val CURVE_NAME = "secp256r1"

object SchnorrSecurity {

    init {
        addProvider(BouncyCastleProvider())
    }

    val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)

    private val g: ECPoint = ecSpec.g
    private val n: BigInteger = ecSpec.n

    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC")
        keyPairGenerator.initialize(ecSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun sign(privateKey: BigInteger, message: ByteArray): Pair<BigInteger, BigInteger> {
        val random = SecureRandom()
        val k = BigInteger(n.bitLength(), random).mod(n)
        val rPoint = g.multiply(k).normalize()
        val r = rPoint.xCoord.toBigInteger()
        val e = hash(r, message).mod(n)
        val s = k.subtract(e.multiply(privateKey)).mod(n)
        return Pair(r, s)
    }

    fun verify(publicKey: ECPoint, message: ByteArray, signature: Pair<BigInteger, BigInteger>): Boolean {
        val (r, s) = signature
        if (r <= BigInteger.ZERO || r >= n) return false
        val e = hash(r, message).mod(n)
        val rPoint = g.multiply(s).add(publicKey.multiply(e)).normalize()
        val rPrime = rPoint.xCoord.toBigInteger()
        return r == rPrime
    }

    private fun hash(r: BigInteger, message: ByteArray): BigInteger {
        val input = r.toByteArray() + message
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return BigInteger(1, digest.digest(input))
    }
}
