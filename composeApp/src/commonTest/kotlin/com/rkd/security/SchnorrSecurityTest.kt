package com.rkd.security

import com.rkd.model.ProjectModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

class SchnorrSecurityTest {

    @Test
    fun `test single key for single project`() {

        val keyPair = SchnorrSecurity.generateKeyPair()
        val privateKey = BigInteger(1, keyPair.private.encoded)
        val publicKey = SchnorrSecurity.ecSpec.g.multiply(privateKey).normalize()

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project 1",
            password = "MY_PASSWORD",
            publicKeys = listOf(publicKey),
            privateKeys = listOf(privateKey)
        )

        assertTrue(project.isValid(publicKey))
    }

    @Test
    fun `test three keys for single project`() {

        val keyPairs = List(3) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 3 keys",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        publicKeys.forEach {
            publicKey -> assertTrue(project.isValid(publicKey))
        }
    }

    @Test
    fun `test three keys but only two are signed`() {

        val keyPairs = List(3) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 3 keys, only 2 signed",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = listOf(privateKeys[0], privateKeys[1])
        )

        assertTrue(project.isValid(publicKeys[0]))
        assertTrue(project.isValid(publicKeys[1]))
        assertFalse(project.isValid(publicKeys[2]))
    }

    @Test
    fun `test five keys but only one is signed`() {

        val keyPairs = List(5) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 5 keys, only 1 signed",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = listOf(privateKeys[0])
        )

        assertTrue(project.isValid(publicKeys[0]))

        publicKeys.drop(1).forEach {
            publicKey -> assertFalse(project.isValid(publicKey))
        }
    }

    @Test
    fun `test remove access for valid key`() {

        val keyPairs = List(3) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        var project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 3 keys",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        val keyToRemove = publicKeys[1]

        project = project.removeAccess(keyToRemove, "MY_PASSWORD")

        assertFalse(project.publicKeys.contains(keyToRemove))
        assertEquals(2, project.publicKeys.size)
        assertFalse(project.isValid(keyToRemove))

        project.publicKeys.forEach { remainingKey ->
            assertTrue(project.isValid(remainingKey))
        }
    }

    @Test
    fun `test remove access with only one key remaining`() {

        val keyPairs = List(1) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 1 key",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        val exception = assertThrows<IllegalStateException> {
            project.removeAccess(publicKeys[0], "MY_PASSWORD")
        }

        assertEquals("Não é possível remover a última chave privada.", exception.message)
    }

    @Test
    fun `test remove access for non-existent key`() {

        val keyPairs = List(2) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        val project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 2 keys",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        val nonExistentKey = SchnorrSecurity.ecSpec.g.multiply(BigInteger.TEN).normalize()

        val exception = assertThrows<IllegalArgumentException> {
            project.removeAccess(nonExistentKey, "MY_PASSWORD")
        }

        assertEquals("Chave pública não encontrada.", exception.message)
    }

    @Test
    fun `test remove access for two keys from five`() {

        val keyPairs = List(5) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        var project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 5 keys",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        val firstKeyToRemove = publicKeys[1]

        project = project.removeAccess(firstKeyToRemove, "MY_PASSWORD")
        assertFalse(project.publicKeys.contains(firstKeyToRemove))
        assertEquals(4, project.publicKeys.size)
        assertFalse(project.isValid(firstKeyToRemove))

        val secondKeyToRemove = publicKeys[2]

        project = project.removeAccess(secondKeyToRemove, "MY_PASSWORD")
        assertFalse(project.publicKeys.contains(secondKeyToRemove))
        assertEquals(3, project.publicKeys.size)
        assertFalse(project.isValid(secondKeyToRemove))

        project.publicKeys.forEach { remainingKey ->
            assertTrue(project.isValid(remainingKey))
        }
    }

    @Test
    fun `test remove access for seven keys from ten`() {

        val keyPairs = List(10) { SchnorrSecurity.generateKeyPair() }
        val privateKeys = keyPairs.map { BigInteger(1, it.private.encoded) }
        val publicKeys = privateKeys.map { SchnorrSecurity.ecSpec.g.multiply(it).normalize() }

        var project = ProjectModel.create(
            name = "PROJECT_1",
            description = "Project with 10 keys",
            password = "MY_PASSWORD",
            publicKeys = publicKeys,
            privateKeys = privateKeys
        )

        val keysToRemove = publicKeys.take(7)

        keysToRemove.forEach { keyToRemove ->
            project = project.removeAccess(keyToRemove, "MY_PASSWORD")
            assertFalse(project.publicKeys.contains(keyToRemove))
            assertFalse(project.isValid(keyToRemove))
        }

        assertEquals(3, project.publicKeys.size)

        project.publicKeys.forEach { remainingKey ->
            assertTrue(project.isValid(remainingKey))
        }
    }
}