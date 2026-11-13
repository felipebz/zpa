//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import oracle.jdbc.pool.OracleDataSource
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.utility.DockerImageName
import java.security.SecureRandom
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import javax.sql.DataSource

/**
 * Disposable users in an Oracle database: the one named by `ZPA_ORACLE_JDBC_URL`, `ZPA_ORACLE_ADMIN_USER` and
 * `ZPA_ORACLE_ADMIN_PASSWORD` when they are set, otherwise a container started once per test run (Testcontainers).
 * Tests are skipped when neither is available.
 */
internal class OracleDatabase private constructor(private val jdbcUrl: String, private val admin: DataSource) : AutoCloseable {
    private val users = ArrayList<String>()
    private val random = SecureRandom()

    /** A new user with the given system privileges; its password is random and never printed. */
    fun createUser(prefix: String, vararg privileges: String): User {
        val name = prefix + "_" + (1..6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        val password = "P" + (1..24).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        admin.connection.use { c ->
            c.createStatement().use { s ->
                s.execute("CREATE USER $name IDENTIFIED BY \"$password\" QUOTA UNLIMITED ON users")
                users += name
                s.execute("GRANT CREATE SESSION" + privileges.joinToString("") { ", $it" } + " TO $name")
            }
        }
        return User(name, dataSource(name, password))
    }

    private fun dataSource(userName: String, secret: String) = OracleDataSource().apply {
        setURL(jdbcUrl)
        this.user = userName
        setPassword(secret)
    }

    override fun close() {
        admin.connection.use { c ->
            c.createStatement().use { s -> users.forEach { s.execute("DROP USER $it CASCADE") } }
        }
    }

    class User(val name: String, val dataSource: DataSource) {
        fun execute(vararg statements: String) = dataSource.connection.use { c ->
            c.createStatement().use { s -> statements.forEach { s.execute(it) } }
        }

        /** True when Oracle compiles and runs the anonymous block. */
        fun accepts(block: String): Boolean = try {
            execute(block)
            true
        } catch (e: SQLException) {
            false
        }

        /** Calls a function-style block `BEGIN ? := …; END;` and returns the string result. */
        fun call(block: String): String = dataSource.connection.use { c ->
            c.prepareCall(block).use { cs ->
                cs.registerOutParameter(1, java.sql.Types.VARCHAR)
                cs.execute()
                cs.getString(1)
            }
        }
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        // Shared by every test class of the run; Testcontainers removes it when the JVM exits. The image (from
        // gradle.properties) has both a tag and a digest, which is not recognized as the module's image.
        private val container: OracleContainer by lazy {
            val image = System.getenv("ZPA_ORACLE_IMAGE") ?: System.getProperty("zpa.oracle.image")
            OracleContainer(DockerImageName.parse(image).asCompatibleSubstituteFor("gvenzl/oracle-free"))
                .withPassword(randomSecret()).withStartupTimeout(Duration.ofMinutes(5)).also { it.start() }
        }

        fun connect(): OracleDatabase {
            val configured = System.getenv("ZPA_ORACLE_JDBC_URL")
            val (jdbcUrl, adminUser, adminPassword) = if (configured != null) {
                Triple(configured, System.getenv("ZPA_ORACLE_ADMIN_USER"), System.getenv("ZPA_ORACLE_ADMIN_PASSWORD"))
            } else {
                assumeTrue(DockerClientFactory.instance().isDockerAvailable, "neither ZPA_ORACLE_JDBC_URL nor Docker is available")
                // The image sets the SYSTEM password from the container's password.
                Triple(container.jdbcUrl, "system", container.password)
            }
            assumeTrue(adminUser != null && adminPassword != null, "ZPA_ORACLE_ADMIN_USER/ADMIN_PASSWORD not set")
            val admin = OracleDataSource().apply {
                setURL(jdbcUrl)
                user = adminUser
                setPassword(adminPassword)
            }
            admin.connection.use { c: Connection -> c.isValid(5) }
            return OracleDatabase(jdbcUrl, admin)
        }

        private fun randomSecret(): String {
            val random = SecureRandom()
            return "P" + (1..24).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        }
    }
}
