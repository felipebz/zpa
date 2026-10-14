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
package com.felipebz.zpa.oracle

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.net.URLClassLoader
import java.nio.file.Path
import java.security.SecureRandom
import java.sql.Connection
import java.sql.Driver
import java.sql.SQLException
import java.util.*

/**
 * Disposable Oracle users and the fixture schema shared by the database-backed tests. The database is reached through
 * `ZPA_ORACLE_JDBC_URL`, `ZPA_ORACLE_ADMIN_USER` and `ZPA_ORACLE_ADMIN_PASSWORD` (tests are skipped when unset); the
 * Oracle JDBC driver is loaded from the JAR given by the `zpa.oracle.driverJar` system property, never from the
 * class path. Passwords are random and never printed.
 */
class OracleFixture private constructor(val jdbcUrl: String, private val adminUser: String, private val adminPassword: String) : AutoCloseable {
    val driverJar: Path = Path.of(System.getProperty("zpa.oracle.driverJar"))
    private val loader = URLClassLoader(arrayOf(driverJar.toUri().toURL()), OracleFixture::class.java.classLoader)
    private val driver = Class.forName("oracle.jdbc.OracleDriver", true, loader).getDeclaredConstructor().newInstance() as Driver
    private val random = SecureRandom()
    private val users = ArrayList<String>()

    /** The fixture owner: tables, a view, a synonym and a package specification compiled by Oracle. */
    lateinit var owner: User
        private set
    /** A second schema with one table the owner can read. */
    lateinit var reference: User
        private set

    class User(val name: String, val password: String, private val fixture: OracleFixture) {
        fun execute(vararg statements: String) = fixture.connect(name, password).use { c ->
            c.createStatement().use { s -> statements.forEach { s.execute(it) } }
        }

        fun accepts(block: String): Boolean = try {
            execute(block)
            true
        } catch (e: SQLException) {
            false
        }

        fun <T> query(sql: String, row: (java.sql.ResultSet) -> T): List<T> = fixture.connect(name, password).use { c ->
            c.createStatement().use { s -> s.executeQuery(sql).use { rs -> generateSequence { if (rs.next()) row(rs) else null }.toList() } }
        }

        override fun toString() = "User($name)"
    }

    fun connect(user: String, password: String): Connection {
        val properties = Properties()
        properties["user"] = user
        properties["password"] = password
        return driver.connect(jdbcUrl, properties)
    }

    fun createUser(prefix: String, vararg privileges: String): User {
        val name = prefix + "_" + (1..6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        val password = "P" + (1..24).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        connect(adminUser, adminPassword).use { c ->
            c.createStatement().use { s ->
                s.execute("CREATE USER $name IDENTIFIED BY \"$password\" QUOTA UNLIMITED ON users")
                users += name
                s.execute("GRANT CREATE SESSION" + privileges.joinToString("") { ", $it" } + " TO $name")
            }
        }
        return User(name, password, this)
    }

    /** Executions of the column dictionary queries parsed in [schema]'s sessions. */
    fun columnQueryExecutions(schema: String): Long = connect(adminUser, adminPassword).use { c ->
        c.prepareStatement(
            "SELECT NVL(SUM(executions), 0) FROM v\$sql WHERE parsing_schema_name = ? " +
                "AND sql_text LIKE 'SELECT owner, table_name, column_name, column_id%'"
        ).use { ps ->
            ps.setString(1, schema)
            ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        }
    }

    /** The PL/SQL sources analyzed by the tests, keyed by file name; the owner compiles [HR_API] successfully. */
    fun sources(): Map<String, String> = mapOf(
        "hr_api.pks" to HR_API,
        "gaps.pks" to """
            CREATE OR REPLACE PACKAGE gaps AS
              v_bonus employees.bonus%TYPE;
              v_synonym emp_syn.salary%TYPE;
              v_city ${reference.name}.cities.name%TYPE;
            END gaps;
            """.trimIndent(),
        "billing.pks" to "CREATE OR REPLACE PACKAGE otherapp.billing AS v_salary employees.salary%TYPE; END billing;",
        "job.sql" to "BEGIN\n  NULL;\nEND;\n/\n"
    ) + (1..6).associate { "p$it.pks" to "CREATE OR REPLACE PACKAGE p$it AS v employees.id%TYPE; END p$it;" }

    private fun createSchema() {
        owner = createUser("ZPASQ_HR", "CREATE TABLE", "CREATE VIEW", "CREATE PROCEDURE", "CREATE SYNONYM")
        reference = createUser("ZPASQ_REF", "CREATE TABLE")
        reference.execute(
            "CREATE TABLE countries (iso CHAR(2 BYTE), name VARCHAR2(60 CHAR))",
            "GRANT SELECT ON countries TO ${owner.name}"
        )
        owner.execute(
            "CREATE TABLE employees (id NUMBER(6) PRIMARY KEY, name VARCHAR2(40 CHAR) NOT NULL, code CHAR(3 BYTE), " +
                "salary NUMBER(10,2), hired DATE, secret VARCHAR2(10) INVISIBLE, note CLOB)",
            "CREATE VIEW emp_v AS SELECT id, name, salary * 12 AS annual FROM employees",
            "CREATE SYNONYM emp_syn FOR employees",
            HR_API
        )
        check(owner.query("SELECT status FROM user_objects WHERE object_name = 'HR_API'") { it.getString(1) } == listOf("VALID"))
    }

    override fun close() {
        try {
            connect(adminUser, adminPassword).use { c -> c.createStatement().use { s -> users.forEach { s.execute("DROP USER $it CASCADE") } } }
        } finally {
            loader.close()
        }
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** Column dictionary queries of one acquisition of the tests' request: the owner schema and one targeted batch. */
        const val COLUMN_QUERIES_PER_ACQUISITION = 2L

        val HR_API = """
            CREATE OR REPLACE PACKAGE hr_api AS
              v_salary employees.salary%TYPE;
              v_name employees.name%TYPE;
              v_hired employees.hired%TYPE;
              r_employee employees%ROWTYPE;
              r_view emp_v%ROWTYPE;
            END hr_api;
            """.trimIndent()

        fun create(): OracleFixture {
            val url = System.getenv("ZPA_ORACLE_JDBC_URL")
            val user = System.getenv("ZPA_ORACLE_ADMIN_USER")
            val password = System.getenv("ZPA_ORACLE_ADMIN_PASSWORD")
            assumeTrue(url != null && user != null && password != null, "ZPA_ORACLE_JDBC_URL/ADMIN_USER/ADMIN_PASSWORD not set")
            val fixture = OracleFixture(url!!, user!!, password!!)
            try {
                fixture.createSchema()
            } catch (e: Throwable) {
                fixture.close()
                throw e
            }
            return fixture
        }
    }
}
