//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class ParameterDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.PARAMETER_DECLARATION)
    }

    @Test
    fun matchesSimpleParameter() {
        assertThat(p).matches("parameter number")
    }

    @Test
    fun matchesExplicitInParameter() {
        assertThat(p).matches("parameter in number")
    }

    @Test
    fun matchesExplicitInParameterWithDefaultValue() {
        assertThat(p).matches("parameter in number := 1")
    }

    @Test
    fun matchesExplicitInParameterWithDefaultValueAlternative() {
        assertThat(p).matches("parameter in number default 1")
    }

    @Test
    fun matchesOutParameter() {
        assertThat(p).matches("x out number")
    }

    @Test
    fun matchesInOutParameter() {
        assertThat(p).matches("x in out number")
    }

    @Test
    fun matchesOutParameterWithNocopy() {
        assertThat(p).matches("x out nocopy number")
    }

    @Test
    fun matchesInOutParameterWithNocopy() {
        assertThat(p).matches("x in out nocopy number")
    }

    @Test
    fun preservesOutParameterModeAsKeyword() {
        val parameter = p.parse("x in out nocopy number")

        assertThatAst(parameter.children.map { it.type })
            .containsExactly(
                PlSqlGrammar.IDENTIFIER_NAME,
                PlSqlKeyword.IN,
                PlSqlKeyword.OUT,
                PlSqlKeyword.NOCOPY,
                PlSqlGrammar.DATATYPE
            )
    }

    @Test
    fun rejectsOutParameterWithoutDatatype() {
        assertThat(p).notMatches("x out")
    }
    @Test
    fun rejectsOutQualifiedDatatype() {
        assertThat(p).notMatches("x out.t")
    }

}
