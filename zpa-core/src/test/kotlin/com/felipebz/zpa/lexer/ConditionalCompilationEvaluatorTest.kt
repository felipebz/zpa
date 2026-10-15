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
package com.felipebz.zpa.lexer

import com.felipebz.zpa.lexer.ConditionResult.FALSE
import com.felipebz.zpa.lexer.ConditionResult.NULL
import com.felipebz.zpa.lexer.ConditionResult.TRUE
import com.felipebz.zpa.lexer.ConditionResult.UNKNOWN
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConditionalCompilationEvaluatorTest {

    private val flags = ConditionalCompilationSettings(
        ConditionalCompilationSettings.parseFlags("t:true, f:false, n:null, one:1, two:2, minus:-3")
    )

    private fun eval(condition: String, settings: ConditionalCompilationSettings = flags) =
        ConditionalCompilationEvaluator.evaluate(condition, settings)

    private fun version(text: String) = ConditionalCompilationSettings(databaseVersion = ConditionalCompilationSettings.parseVersion(text))

    @Test
    fun literalsAndFlags() {
        assertThat(eval("TRUE")).isEqualTo(TRUE)
        assertThat(eval("false")).isEqualTo(FALSE)
        assertThat(eval("NULL")).isEqualTo(NULL)
        assertThat(eval("\$\$t")).isEqualTo(TRUE)
        assertThat(eval("\$\$F")).isEqualTo(FALSE)
        assertThat(eval("\$\$n")).isEqualTo(NULL)
        // An unconfigured name is NULL (PLW-6003); a predefined one is not known.
        assertThat(eval("\$\$not_set")).isEqualTo(NULL)
        listOf("PLSQL_LINE", "plsql_unit", "PLSQL_UNIT_OWNER", "PLSQL_UNIT_TYPE", "PLSCOPE_SETTINGS", "PLSQL_CCFLAGS",
            "PLSQL_CODE_TYPE", "PLSQL_OPTIMIZE_LEVEL", "PLSQL_WARNINGS", "NLS_LENGTH_SEMANTICS", "PERMIT_92_WRAP_FORMAT")
            .forEach { assertThat(eval("\$\$$it = 1")).describedAs(it).isEqualTo(UNKNOWN) }
        // An integer alone is not a boolean condition.
        assertThat(eval("\$\$one")).isEqualTo(UNKNOWN)
    }

    @Test
    fun kleeneLogic() {
        val values = mapOf("TRUE" to TRUE, "FALSE" to FALSE, "NULL" to NULL, "pkg.c" to UNKNOWN)
        val and = mapOf(
            "TRUE TRUE" to TRUE, "TRUE FALSE" to FALSE, "TRUE NULL" to NULL, "TRUE pkg.c" to UNKNOWN,
            "FALSE FALSE" to FALSE, "FALSE NULL" to FALSE, "FALSE pkg.c" to FALSE,
            "NULL NULL" to NULL, "NULL pkg.c" to UNKNOWN, "pkg.c pkg.c" to UNKNOWN
        )
        val or = mapOf(
            "TRUE TRUE" to TRUE, "TRUE FALSE" to TRUE, "TRUE NULL" to TRUE, "TRUE pkg.c" to TRUE,
            "FALSE FALSE" to FALSE, "FALSE NULL" to NULL, "FALSE pkg.c" to UNKNOWN,
            "NULL NULL" to NULL, "NULL pkg.c" to UNKNOWN, "pkg.c pkg.c" to UNKNOWN
        )
        for ((pair, expected) in and) {
            val (a, b) = pair.split(' ')
            assertThat(eval("$a AND $b")).describedAs("$a AND $b").isEqualTo(expected)
            assertThat(eval("$b AND $a")).describedAs("$b AND $a").isEqualTo(expected)
        }
        for ((pair, expected) in or) {
            val (a, b) = pair.split(' ')
            assertThat(eval("$a OR $b")).describedAs("$a OR $b").isEqualTo(expected)
            assertThat(eval("$b OR $a")).describedAs("$b OR $a").isEqualTo(expected)
        }
        assertThat(eval("NOT TRUE")).isEqualTo(FALSE)
        assertThat(eval("NOT FALSE")).isEqualTo(TRUE)
        assertThat(eval("NOT NULL")).isEqualTo(NULL)
        assertThat(eval("NOT pkg.c")).isEqualTo(UNKNOWN)
        assertThat(values).hasSize(4)
    }

    @Test
    fun comparisonsAndArithmetic() {
        assertThat(eval("\$\$one = 1")).isEqualTo(TRUE)
        listOf("<>", "!=", "~=", "^=").forEach { assertThat(eval("\$\$one $it 2")).describedAs(it).isEqualTo(TRUE) }
        assertThat(eval("\$\$one < \$\$two")).isEqualTo(TRUE)
        assertThat(eval("\$\$two <= 2")).isEqualTo(TRUE)
        assertThat(eval("\$\$two > 2")).isEqualTo(FALSE)
        assertThat(eval("\$\$two >= 3")).isEqualTo(FALSE)
        assertThat(eval("\$\$minus = -3")).isEqualTo(TRUE)
        assertThat(eval("-\$\$minus = +3")).isEqualTo(TRUE)
        assertThat(eval("\$\$one + \$\$two * 3 = 7")).isEqualTo(TRUE)
        assertThat(eval("(\$\$one + \$\$two) * 3 = 9")).isEqualTo(TRUE)
        assertThat(eval("10 - 2 - 3 = 5")).isEqualTo(TRUE)
        assertThat(eval("TRUE = TRUE")).isEqualTo(TRUE)
        assertThat(eval("FALSE < TRUE")).isEqualTo(TRUE)
        assertThat(eval("\$\$n = 1")).isEqualTo(NULL)
        assertThat(eval("\$\$not_set = 1")).isEqualTo(NULL)
        assertThat(eval("\$\$not_set + 1 = 1")).isEqualTo(NULL)
        assertThat(eval("NULL = NULL")).isEqualTo(NULL)
        assertThat(eval("\$\$n IS NULL")).isEqualTo(TRUE)
        assertThat(eval("\$\$one IS NULL")).isEqualTo(FALSE)
        assertThat(eval("\$\$one IS NOT NULL")).isEqualTo(TRUE)
        assertThat(eval("\$\$not_set IS NOT NULL")).isEqualTo(FALSE)
        // Type mismatch.
        assertThat(eval("TRUE > 1")).isEqualTo(UNKNOWN)
        assertThat(eval("1 AND TRUE")).isEqualTo(UNKNOWN)
        assertThat(eval("TRUE + 1 = 2")).isEqualTo(UNKNOWN)
    }

    @Test
    fun precedence() {
        // NOT binds looser than comparison, AND tighter than OR.
        assertThat(eval("NOT \$\$one = 2")).isEqualTo(TRUE)
        assertThat(eval("TRUE OR TRUE AND FALSE")).isEqualTo(TRUE)
        assertThat(eval("(TRUE OR TRUE) AND FALSE")).isEqualTo(FALSE)
        assertThat(eval("NOT FALSE AND FALSE")).isEqualTo(FALSE)
        assertThat(eval("-2 * 3 + 1 = -5")).isEqualTo(TRUE)
    }

    @Test
    fun commentsInsideTheCondition() {
        assertThat(eval("\$\$t -- line comment\n AND /* block */ \$\$one = 1")).isEqualTo(TRUE)
    }

    @Test
    fun databaseVersion() {
        val v19 = version("19")
        assertThat(eval("DBMS_DB_VERSION.VERSION = 19", v19)).isEqualTo(TRUE)
        assertThat(eval("dbms_db_version.release = 0", v19)).isEqualTo(TRUE)
        assertThat(eval("SYS.DBMS_DB_VERSION.VERSION >= 12", v19)).isEqualTo(TRUE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_19", v19)).isEqualTo(TRUE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_18", v19)).isEqualTo(FALSE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_19_0", v19)).isEqualTo(TRUE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_12_2", version("12.2"))).isEqualTo(TRUE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_12_1", version("12.2"))).isEqualTo(FALSE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_12_2", version("12.1"))).isEqualTo(TRUE)
        assertThat(eval("DBMS_DB_VERSION.VER_LE_11", version("12.1"))).isEqualTo(FALSE)
        // No version configured.
        assertThat(eval("DBMS_DB_VERSION.VER_LE_19")).isEqualTo(UNKNOWN)
        assertThat(eval("DBMS_DB_VERSION.VERSION = 19")).isEqualTo(UNKNOWN)
        assertThat(eval("DBMS_DB_VERSION.SOMETHING_ELSE", v19)).isEqualTo(UNKNOWN)
    }

    @Test
    fun unsupportedConstructsAreUnknown() {
        listOf(
            "my_debug.debug", "f(1) = 1", "'a' = 'a'", "\$\$two / 2 = 1", "\$\$two ** 2 = 4", "MOD(\$\$two, 2) = 0",
            "\$\$two MOD 2 = 0", "TRUE XOR FALSE", "\$\$x LIKE 'a%'", "\"Q\"", "1.5 = 1.5", "(", "TRUE TRUE", "", "\$\$t AND"
        ).forEach { assertThat(eval(it)).describedAs(it).isEqualTo(UNKNOWN) }
        // Sound partial evaluation around an unknown operand.
        assertThat(eval("\$\$f AND my_debug.debug")).isEqualTo(FALSE)
        assertThat(eval("\$\$t OR my_debug.debug")).isEqualTo(TRUE)
    }
}
