//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.grammar

import com.felipebz.flr.internal.vm.CompilableGrammarRule
import com.felipebz.flr.internal.vm.MutableGrammarCompiler
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class GrammarSizeTest {

    // Every parser test compiles the grammar, so growth here multiplies the cost of the whole suite.
    @Test
    fun compiledGrammarStaysWithinItsInstructionBudget() {
        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
        val root = parser.grammar.rule(PlSqlGrammar.FILE_INPUT) as CompilableGrammarRule

        val instructions = MutableGrammarCompiler.compile(root).instructions.size

        assertThat(instructions)
            .describedAs("compiled PlSqlGrammar instructions (about 51,600 when the budget was set)")
            .isLessThanOrEqualTo(57_000)
    }
}
