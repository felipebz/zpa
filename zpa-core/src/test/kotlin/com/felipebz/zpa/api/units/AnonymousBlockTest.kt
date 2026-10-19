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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatValue
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import com.felipebz.zpa.api.ConditionsGrammar

class AnonymousBlockTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ANONYMOUS_BLOCK)
    }

    @Test
    fun matchesSimpleBlock() {
        assertThat(p).matches(""
                + "BEGIN\n"
                + "NULL;\n"
                + "END;")
    }

    @Test
    fun matchesBlockWithDeclareSection() {
        assertThat(p).matches(""
                + "DECLARE\n"
                + "VAR NUMBER;\n"
                + "BEGIN\n"
                + "NULL;\n"
                + "END;")
    }

    @Test
    fun matchesTreatAndIsOfTypesAsDatatypesInPlSqlAndStaticSql() {
        val block = "" +
            "declare s shape_t; r number; begin\n" +
            "  r := treat(s as circle_t).r;\n" +
            "  if s is of (only circle_t) then null; end if;\n" +
            "  select treat(h.s as hr.circle_t).r into r from holder h where h.s is of type (only circle_t, square_t);\n" +
            "end;"
        assertThat(p).matches(block)
        val tree = p.parse(block)
        for (node in tree.getDescendants(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION)) {
            assertThatValue(node.getFirstChild(PlSqlGrammar.DATATYPE)?.firstChild?.type).isEqualTo(PlSqlGrammar.CUSTOM_DATATYPE)
        }
        assertThatValue(tree.getDescendants(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION)).hasSize(2)
        val isOf = tree.getDescendants(ConditionsGrammar.IS_OF_CONDITION)
        assertThatValue(isOf.map { condition -> condition.getChildren(PlSqlGrammar.DATATYPE).size }).containsExactly(1, 2)

        val outsideDatatypes = tree.getDescendants(PlSqlGrammar.VARIABLE_NAME)
            .filter { it.getFirstAncestorOrNull(PlSqlGrammar.DATATYPE) == null }.map { it.tokenValue.lowercase() }
        assertThatValue(outsideDatatypes).doesNotContain("circle_t", "square_t", "hr")
    }

}
