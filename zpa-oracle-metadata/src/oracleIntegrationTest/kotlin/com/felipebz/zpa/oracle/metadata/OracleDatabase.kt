/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.oracle.metadata

import oracle.jdbc.pool.OracleDataSource
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.security.SecureRandom
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Disposable users in an Oracle database reached through environment variables, never through literals:
 * `ZPA_ORACLE_JDBC_URL`, `ZPA_ORACLE_ADMIN_USER`, `ZPA_ORACLE_ADMIN_PASSWORD`. Tests are skipped when they are unset.
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

        fun connect(): OracleDatabase {
            val jdbcUrl = System.getenv("ZPA_ORACLE_JDBC_URL")
            val adminUser = System.getenv("ZPA_ORACLE_ADMIN_USER")
            val adminPassword = System.getenv("ZPA_ORACLE_ADMIN_PASSWORD")
            assumeTrue(jdbcUrl != null && adminUser != null && adminPassword != null, "ZPA_ORACLE_JDBC_URL/ADMIN_USER/ADMIN_PASSWORD not set")
            val admin = OracleDataSource().apply {
                setURL(jdbcUrl)
                user = adminUser
                setPassword(adminPassword)
            }
            admin.connection.use { c: Connection -> c.isValid(5) }
            return OracleDatabase(jdbcUrl!!, admin)
        }
    }
}
