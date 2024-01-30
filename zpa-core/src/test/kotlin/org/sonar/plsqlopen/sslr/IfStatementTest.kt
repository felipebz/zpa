/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.sslr

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar

class IfStatementTest : TreeTest<IfStatement>(PlSqlGrammar.IF_STATEMENT) {

    @Test
    fun matchesSimpleIf() {
        val tree = parse(
            "if true then "
            + "null; "
            + "end if;")

        assertThat(tree.condition.allTokensToString).isEqualTo("true")
        assertThat(tree.statements).hasSize(1)
        assertThat(tree.statements[0].tree).isInstanceOf(NullStatement::class.java)
        assertThat(tree.elsifClauses).hasSize(0)
        assertThat(tree.elseClause).isNull()
    }

    @Test
    fun matchesIfWithElsif() {
        val tree = parse(
            "if true then "
            + "null; "
            + "elsif x = 1 then "
            + "null; "
            + "end if;")

        assertThat(tree.condition.allTokensToString).isEqualTo("true")
        assertThat(tree.statements).hasSize(1)
        assertThat(tree.statements[0].tree).isInstanceOf(NullStatement::class.java)

        assertThat(tree.elsifClauses).hasSize(1)
        assertThat(tree.elsifClauses[0].condition.allTokensToString).isEqualTo("x = 1")
        assertThat(tree.elsifClauses[0].statements).hasSize(1)
        assertThat(tree.elsifClauses[0].statements[0].tree).isInstanceOf(NullStatement::class.java)

        assertThat(tree.elseClause).isNull()
    }


    @Test
    fun matchesIfWithElsifAndElse() {
        val tree = parse(
            "if true then "
            + "null; "
            + "elsif x = 1 then "
            + "null; "
            + "elsif x = 2 then "
            + "null; "
            + "else "
            + "null; "
            + "end if;")

        assertThat(tree.condition.allTokensToString).isEqualTo("true")
        assertThat(tree.statements).hasSize(1)
        assertThat(tree.statements[0].tree).isInstanceOf(NullStatement::class.java)

        assertThat(tree.elsifClauses).hasSize(2)
        assertThat(tree.elsifClauses[0].condition.allTokensToString).isEqualTo("x = 1")
        assertThat(tree.elsifClauses[0].statements).hasSize(1)
        assertThat(tree.elsifClauses[0].statements[0].tree).isInstanceOf(NullStatement::class.java)

        assertThat(tree.elsifClauses[1].condition.allTokensToString).isEqualTo("x = 2")
        assertThat(tree.elsifClauses[1].statements).hasSize(1)
        assertThat(tree.elsifClauses[1].statements[0].tree).isInstanceOf(NullStatement::class.java)

        assertThat(tree.elseClause).isNotNull
        assertThat(tree.elseClause!!.statements).hasSize(1)
        assertThat(tree.elseClause!!.statements[0].tree).isInstanceOf(NullStatement::class.java)
    }

}
