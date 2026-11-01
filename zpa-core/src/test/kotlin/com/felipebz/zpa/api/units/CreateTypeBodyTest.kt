//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatValue

class CreateTypeBodyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_TYPE_BODY)
    }

    @Test
    fun matchesEditionableTypeBody() {
        assertThat(p).matches(
                "create editionable type body foo as "
                        + "member procedure foo is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesNonEditionableTypeBody() {
        assertThat(p).matches(
                "create noneditionable type body foo as "
                        + "member procedure foo is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesTypeBodyWithProcedure() {
        assertThat(p).matches(
                "create type body foo as "
                        + "member procedure foo is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesTypeBodyWithFunction() {
        assertThat(p).matches(
                "create type body foo is "
                        + "member function foo return number is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesTypeBodyWithStaticFunction() {
        assertThat(p).matches(
                "create type body foo is "
                        + "static function foo return number is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesTypeBodyWithConstructor() {
        assertThat(p).matches(
                "create type body foo is "
                        + "constructor function bar return self as result as "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesTypeBodyWithOverridenFunction() {
        assertThat(p).matches(
                "create type body foo is "
                        + "overriding member function foo return number is "
                        + "begin null; end; "
                        + "end;")
    }


    @Test
    fun matchesTypeBodyWithOverridenFinalFunction() {
        assertThat(p).matches(
                "create type body foo is "
                        + "overriding final member function foo return number is "
                        + "begin null; end; "
                        + "end;")
    }

    @Test
    fun matchesGeneralizedInvocationWithDatatype() {
        val body = "" +
            "create or replace type body circle_t as " +
            "overriding member function area return number is " +
            "begin return (self as shape_t).area() + (self as hr.shape_t).area(); end; " +
            "end;"
        assertThat(p).matches(body)
        val types = p.parse(body).getDescendants(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION)
            .map { it.getFirstChild(PlSqlGrammar.DATATYPE)?.firstChild?.type }
        assertThatValue(types).containsExactly(PlSqlGrammar.CUSTOM_DATATYPE, PlSqlGrammar.CUSTOM_DATATYPE)
    }

}
