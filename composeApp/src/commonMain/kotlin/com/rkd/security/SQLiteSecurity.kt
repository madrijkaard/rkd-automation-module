package com.rkd.security

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection

object SQLiteSecurity {

    private val logger = LoggerFactory.getLogger(SQLiteSecurity::class.java)

    private const val DB_DIRECTORY = "../deployment/db"
    private const val DB_NAME = "prepare-automation-module.db"
    private const val DB_FILE_PATH = "$DB_DIRECTORY/$DB_NAME"
    private const val PASSWORD = "YourStrongPassword123"

    private val dataSource: HikariDataSource by lazy {
        initializeDataSource()
    }

    fun initializeDatabase(): Connection {

        try {

            val dbDir = File(DB_DIRECTORY)

            if (!dbDir.exists()) {
                logger.info("Directory $DB_DIRECTORY was not found. Creating the directory...")
                dbDir.mkdirs()
            }

            val encryptedFile = File("$DB_FILE_PATH.encrypted")

            if (encryptedFile.exists()) {

                try {

                    logger.info("Banco de dados criptografado encontrado. Descriptografando...")

                    FileSecurity.decrypt(encryptedFile.absolutePath, PASSWORD)

                    if (!encryptedFile.delete()) {
                        logger.warn("Não foi possível excluir o arquivo criptografado em ${encryptedFile.absolutePath}.")
                    } else {
                        logger.info("Arquivo criptografado ${encryptedFile.absolutePath} excluído após descriptografia.")
                    }

                } catch (e: Exception) {
                    logger.error("Erro ao descriptografar o banco de dados: ${e.message}", e)
                    throw RuntimeException("Falha ao descriptografar o banco de dados.", e)
                }
            } else {

                val dbFile = File(DB_FILE_PATH)

                if (!dbFile.exists()) {
                    logger.info("Arquivo de banco de dados $DB_FILE_PATH não encontrado. Criando arquivo...")
                    dbFile.createNewFile()
                } else {
                    logger.info("Arquivo de banco de dados encontrado em $DB_FILE_PATH.")
                }
            }

            return getConnection()

        } catch (e: Exception) {
            logger.error("Erro ao inicializar o banco de dados: ${e.message}", e)
            throw RuntimeException("Erro ao inicializar o banco de dados", e)
        }
    }

    private fun initializeDataSource(): HikariDataSource {

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$DB_FILE_PATH"
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 60000
            maxLifetime = 1800000
            connectionTimeout = 30000
        }

        return HikariDataSource(config).also {
            logger.info("HikariCP DataSource configurado com sucesso!")
        }
    }

    fun getConnection(): Connection {
        return try {
            dataSource.connection
        } catch (e: Exception) {
            logger.error("Erro ao obter conexão do pool: ${e.message}", e)
            throw e
        }
    }

    fun shutdownDatabase() {

        try {

            if (!dataSource.isClosed) {
                dataSource.close()
                logger.info("HikariCP DataSource fechado.")
            } else {
                logger.warn("HikariCP DataSource já estava fechado.")
            }

            val dbFile = File(DB_FILE_PATH)

            if (dbFile.exists() && !dbFile.name.endsWith(".encrypted")) {

                try {

                    logger.info("Criptografando banco de dados antes de encerrar...")

                    FileSecurity.encrypt(dbFile.absolutePath, PASSWORD)

                    if (!dbFile.delete()) {
                        logger.warn("Não foi possível excluir o arquivo descriptografado em ${dbFile.absolutePath}.")
                    }

                } catch (e: Exception) {
                    logger.error("Erro ao criptografar o banco de dados: ${e.message}", e)
                }
            }

        } catch (e: Exception) {
            logger.error("Erro ao encerrar o banco de dados: ${e.message}", e)
        }
    }
}