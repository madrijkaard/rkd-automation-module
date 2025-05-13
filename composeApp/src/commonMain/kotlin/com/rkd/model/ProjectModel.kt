package com.rkd.model

import com.rkd.security.Argon2Security
import com.rkd.security.CompressionSecurity
import com.rkd.security.SchnorrSecurity
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

class ProjectModel private constructor(
    val id: Long? = null,
    val name: String,
    val description: String,
    val seed: String,
    val aggregatedSignatures: List<Pair<BigInteger, BigInteger>>,
    val publicKeys: List<ECPoint>,
    val status: Char,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {

    companion object {

        fun create(
            name: String,
            description: String,
            password: String,
            status: Char = 'A',
            publicKeys: List<ECPoint>,
            privateKeys: List<BigInteger>
        ): ProjectModel {

            val compressedPrivateKeys = CompressionSecurity.compress(privateKeys)

            val encryptedAndCompressedSeed = Argon2Security.encrypt(compressedPrivateKeys, password)

            val aggregatedSignatures = List(publicKeys.size) { index ->

                val privateKey = privateKeys.getOrNull(index)

                if (privateKey != null) {
                    SchnorrSecurity.sign(privateKey, encryptedAndCompressedSeed.toByteArray())
                } else {
                    Pair(BigInteger.ZERO, BigInteger.ZERO)
                }
            }

            return ProjectModel(
                name = name,
                description = description,
                seed = encryptedAndCompressedSeed,
                status = status,
                aggregatedSignatures = aggregatedSignatures,
                publicKeys = publicKeys
            )
        }
    }

    fun removeAccess(userPublicKey: ECPoint, password: String): ProjectModel {

        val compressedPrivateKeys = Argon2Security.decrypt(seed, password)
        val privateKeys = CompressionSecurity.decompress(compressedPrivateKeys)

        val keyIndex = publicKeys.indexOf(userPublicKey)

        require(keyIndex != -1) { "Chave pública não encontrada." }

        check(privateKeys.size > 1) { "Não é possível remover a última chave privada." }

        val updatedPrivateKeys = privateKeys.toMutableList().apply { removeAt(keyIndex) }
        val updatedPublicKeys = publicKeys.toMutableList().apply { removeAt(keyIndex) }

        val updatedCompressedPrivateKeys = CompressionSecurity.compress(updatedPrivateKeys)
        val updatedEncryptedSeed = Argon2Security.encrypt(updatedCompressedPrivateKeys, password)

        val updatedAggregatedSignatures = List(updatedPublicKeys.size) { index ->

            val privateKey = updatedPrivateKeys.getOrNull(index)

            if (privateKey != null) {
                SchnorrSecurity.sign(privateKey, updatedEncryptedSeed.toByteArray())
            } else {
                Pair(BigInteger.ZERO, BigInteger.ZERO)
            }
        }

        return ProjectModel(
            id = id,
            name = name,
            description = description,
            seed = updatedEncryptedSeed,
            aggregatedSignatures = updatedAggregatedSignatures,
            publicKeys = updatedPublicKeys,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun isValid(userPublicKey: ECPoint): Boolean {

        publicKeys.forEachIndexed { index, publicKey ->

            if (publicKey == userPublicKey) {

                val signature = aggregatedSignatures[index]

                return SchnorrSecurity.verify(publicKey, seed.toByteArray(), signature)
            }
        }

        return false
    }
}