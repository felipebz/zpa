//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.grammar

import com.felipebz.flr.internal.vm.Instruction
import com.felipebz.flr.internal.vm.InstructionProgram
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JsonArrayStepAdmissionExpressionTest {
    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val program = InstructionProgram.link(arrayOf(
        JsonArrayStepAdmissionExpression,
        RequireUnconsumedInputExpression,
        Instruction.end()
    ))

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
        return Machine.execute(program, *tokens)
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
