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
package com.felipebz.zpa.grammar

import com.felipebz.flr.internal.vm.Instruction
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JsonArrayStepAdmissionExpressionTest {
    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

    @Test
    fun admitsJsonArrayStepPrefixes() {
        listOf(
            "t.data[0]",
            "t.data.a[0]",
            "t.data.a[*]",
            "\"t\".\"data\"[0]"
        ).forEach { source ->
            assertThat(admits(source)).describedAs(source).isTrue()
        }
    }

    @Test
    fun rejectsOrdinaryQualifiedExpressions() {
        listOf(
            "a.b",
            "a.b.c",
            "a.b(1)",
            "package.function(1)",
            "table_name.column"
        ).forEach { source ->
            assertThat(admits(source)).describedAs(source).isFalse()
        }
    }

    @Test
    fun safelyRejectsShortInputs() {
        listOf(
            "a",
            "a.",
            "a.b",
            "a.b."
        ).forEach { source ->
            assertThat(admits(source)).describedAs(source).isFalse()
        }
    }

    @Test
    fun ignoresWhitespaceAndCommentTrivia() {
        listOf(
            "t . data [0]",
            "t/* qualifier */.data/* array step */[0]",
            "t.data /* property */ . a /* array step */ [*]"
        ).forEach { source ->
            assertThat(admits(source)).describedAs(source).isTrue()
        }
    }

    private fun admits(source: String): Boolean {
        val tokens = lexer.lex(source).toTypedArray()
        return Machine.execute(
            arrayOf(
                JsonArrayStepAdmissionExpression,
                RequireUnconsumedInputExpression,
                Instruction.end()
            ),
            *tokens
        )
    }

    private object RequireUnconsumedInputExpression : NativeExpression() {
        override fun execute(machine: Machine) {
            if (machine.index == 0) {
                machine.jump(1)
            } else {
                machine.backtrack()
            }
        }
    }
}
