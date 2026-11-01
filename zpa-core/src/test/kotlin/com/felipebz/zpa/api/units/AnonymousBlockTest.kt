//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatValue

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
