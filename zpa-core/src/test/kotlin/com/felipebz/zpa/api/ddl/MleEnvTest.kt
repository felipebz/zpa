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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class MleEnvTest : RuleTest() {

    private fun create(vararg statements: String) {
        setRootRule(DdlGrammar.CREATE_MLE_ENV)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notCreate(vararg statements: String) {
        setRootRule(DdlGrammar.CREATE_MLE_ENV)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    private fun alter(vararg statements: String) {
        setRootRule(DdlGrammar.ALTER_MLE_ENV)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notAlter(vararg statements: String) {
        setRootRule(DdlGrammar.ALTER_MLE_ENV)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    @Test
    fun matchesCreateForms() {
        create(
            "create mle env e",
            "create mle env e;",
            "create mle env scott.\"myenv\"",
            "create or replace mle env e",
            "create mle env if not exists e",
            "create or replace mle env if not exists e",
            "create mle env e pure",
            "create mle env e clone other",
            "create mle env e clone scott.\"other_env\"",
            "create mle env e language options 'js.strict=true'",
            "create mle env e language options ''",
            "create mle env e language options q'[a=b]'",
            "create mle env e language options 'js.strict=true' pure",
            "create mle env e imports ('a' module m1)",
            "create mle env e imports ('a' module scott.m1, 'b' module m2)",
            "create mle env e imports ('a' module m1) language options 'x=1'",
            "create mle env e imports ('a' module m1) language options 'x=1' pure",
            "create mle env e imports ('a' module m1) pure"
        )
    }

    @Test
    fun rejectsMalformedCreate() {
        notCreate(
            "create mle env",
            "create mle env a.b.c",
            "create mle env e if not exists",
            "create mle env e clone",
            "create mle env e clone 'other'",
            "create mle env e clone other pure",
            "create mle env e clone other imports ('a' module m1)",
            "create mle env e clone other language options 'x'",
            "create mle env e imports ('a' module m1) clone other",
            "create mle env e language options 'x' clone other",
            "create mle env e pure clone other",
            "create mle env e pure pure",
            "create mle env e pure language options 'x'",
            "create mle env e language options 'x' imports ('a' module m1)",
            "create mle env e language options 'a' language options 'b'",
            "create mle env e imports ('a' module m1) imports ('b' module m2)",
            "create mle env e language options x",
            "create mle env e language options 'a' || 'b'",
            "create mle env e language options",
            "create mle env e language",
            "create mle env e imports",
            "create mle env e imports ()",
            "create mle env e imports ('a' module m1,)",
            "create mle env e imports ('a' module m1 'b' module m2)",
            "create mle env e imports (('a' module m1))",
            "create mle env e imports (('a' module m1), ('b' module m2))",
            "create mle env e imports ('a' module)",
            "create mle env e imports ('a' module 'm1')",
            "create mle env e imports ('a' m1)",
            "create mle env e imports (a module m1)",
            "create mle env e imports 'a' module m1",
            "create mle env e imports ('a' module a.b.c)",
            "create mle env e x",
            "create public mle env e",
            "create editionable mle env e",
            "create mle e"
        )
    }

    @Test
    fun matchesAlterOperations() {
        alter(
            "alter mle env scott.\"myenv\" set language options 'js.strict=\ntrue '",
            "alter mle env e set language options 'x=1'",
            "alter mle env e set language options ''",
            "alter mle env e compile",
            "alter mle env e compile;",
            "alter mle env if exists e compile",
            "alter mle env scott.e add imports ('a' module m1)",
            "alter mle env e add imports ('a' module m1, 'b' module scott.m2)",
            "alter mle env e alter imports ('a' module m1)",
            "alter mle env e alter imports ('a' module m1, 'b' module m2)",
            "alter mle env e drop imports ('a')",
            "alter mle env e drop imports ('a', 'b', 'c')"
        )
    }

    @Test
    fun rejectsMalformedAlter() {
        notAlter(
            "alter mle env e",
            "alter mle env compile",
            "alter mle env a.b.c compile",
            "alter mle env e if exists compile",
            "alter mle env e compile debug",
            "alter mle env e compile reuse settings",
            "alter mle env e compile x",
            "alter mle env e compile set language options 'x'",
            "alter mle env e add imports ('a' module m1) drop imports ('b')",
            "alter mle env e add imports ('a' module m1), set language options 'x'",
            "alter mle env e add ('a' module m1)",
            "alter mle env e add imports",
            "alter mle env e add imports ()",
            "alter mle env e add imports ('a' module m1,)",
            "alter mle env e add imports (('a' module m1), ('b' module m2))",
            "alter mle env e add imports (a module m1)",
            "alter mle env e add imports ('a' module)",
            "alter mle env e alter imports ('a')",
            "alter mle env e drop imports",
            "alter mle env e drop imports ()",
            "alter mle env e drop imports (a)",
            "alter mle env e drop imports ('a',)",
            "alter mle env e drop imports ('a' module m1)",
            "alter mle env e set language options",
            "alter mle env e set language options x",
            "alter mle env e set imports ('a' module m1)",
            "alter mle env e language options 'x'",
            "alter mle env e pure",
            "alter mle env e rename to e2"
        )
    }

    @Test
    fun exposesStatementNodesAndKeepsKeywordsUsableAsNames() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create mle env e imports ('a' module m1) pure; " +
            "alter mle env e compile; " +
            "create table env (mle number, imports number, module number, pure number);")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_MLE_ENV)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_MLE_ENV)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_TABLE)).hasSize(1)
        assertThat(p).matches("create mle env env")
    }
}
