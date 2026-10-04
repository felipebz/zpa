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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TriggerCallBodyAndReferencesTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_TRIGGER)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    private val head = "create or replace trigger t before delete on tab for each row"

    @Test
    fun matchesCallBodies() {
        listOf(
            "call p(:old.id, :old.ename)", "call p(:old.id, :old.ename);", "call p", "call p(1)", "call pkg.p(1)", "call s.pkg.p(1)", "call s.pkg.p",
            "call p(1 + 1)", "call p(:new.a || 'x')", "call p(f(1))", "call p(a => 1)", "call p(case when 1 = 1 then 1 end)",
            "when (1 = 1) call p(1)", "enable call p(1)", "follows t2 call p(1)", "call Before_delete (:OLD.Id, :OLD.Ename)",
        ).forEach {
            val source = "$head $it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "create trigger t before delete on tab call p(1)", "create trigger t after insert on tab call p(1);",
            "create trigger t instead of delete on tab for each row call p(1)",
            "create trigger t after logon on database call p(1)", "create trigger t before ddl on schema call p(1)",
            "create trigger t before insert on tab referencing new as n for each row call p(:n.a)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedCallBodies() {
        listOf(
            "call", "call p()", "call p(1,)", "call p(,)", "call p(1) into :x", "call p(1) into x", "call a.b(1).c(2)", "call p(1) p(2)", "call a.b.c.d(1)", "call a.b.c.d.e(1)", "call a.b.c.d", "call p(1).x",
            "call p(1), q(2)", "call 1", "call 'p'", "call p@lnk(1)", "call p(1) begin null; end;", "call p(:old.id) when (1 = 1)",
            "call p(1) declare x number; begin null; end;", "call (p)(1)", "declare x number; call p(1)", "begin call p(1); end;",
        ).forEach {
            val source = "$head $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).notMatches("create trigger t for delete on tab compound trigger call p(1)")
    }

    @Test
    fun buildsCallBodyNodeAndKeepsBlockBodiesUnchanged() {
        val call = p.parse("$head call Before_delete (:OLD.Id, :OLD.Ename);")
        val body = call.getFirstDescendant(PlSqlGrammar.TRIGGER_CALL_BODY)
        assertThatAst(body.getChildren(PlSqlGrammar.ARGUMENT)).hasSize(2)
        assertThatAst(body.texts().first()).isEqualTo("call")
        assertThatAst(call.getDescendants(PlSqlGrammar.STATEMENTS_SECTION)).isEmpty()

        listOf(
            "$head begin null; end;", "$head declare x number; begin null; end;", "$head when (1 = 1) begin null; end;",
        ).forEach {
            val block = p.parse(it)
            assertThatAst(block.getDescendants(PlSqlGrammar.TRIGGER_CALL_BODY)).describedAs(it).isEmpty()
            assertThatAst(block.getDescendants(PlSqlGrammar.STATEMENTS_SECTION)).describedAs(it).hasSize(1)
        }
        assertThatAst(p.parse("$head declare x number; begin null; end;").getFirstDescendant(PlSqlGrammar.SIMPLE_DML_TRIGGER)
            .hasDirectChildren(PlSqlGrammar.DECLARE_SECTION)).isTrue()
    }

    @Test
    fun matchesReferencesAndReferencingSpellings() {
        listOf("referencing", "references").forEach { keyword ->
            listOf(
                "$keyword for each row begin null; end;", "$keyword new as n for each row begin null; end;",
                "$keyword new n for each row begin null; end;", "$keyword old as o new as n for each row begin null; end;",
                "$keyword new as n old as o parent as pa for each row begin null; end;", "$keyword begin null; end;",
                "$keyword old as o old as o2 for each row begin null; end;", "$keyword new as n for each row call p(:n.a)",
                "$keyword for each row call p(1)", "$keyword call p(1)",
            ).forEach {
                val source = "create trigger t before insert on tab $it"
                assertThat(p).describedAs(source).matches(source)
            }
        }
        listOf(
            "references bogus for each row begin null; end;", "references new as n, old as o for each row begin null; end;",
            "references new as n references old as o for each row begin null; end;",
            "referencing new as n references old as o for each row begin null; end;",
            "references as n for each row begin null; end;", "references new as for each row begin null; end;",
            "for each row references new as n begin null; end;", "references",
        ).forEach {
            val source = "create trigger t before insert on tab $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun buildsReferencingClauseForBothSpellings() {
        listOf("referencing", "references").forEach { keyword ->
            val clause = p.parse("create trigger t before insert on tab $keyword new as n old o for each row begin null; end;")
                .getFirstDescendant(PlSqlGrammar.REFERENCING_CLAUSE)
            assertThatAst(clause.texts()).containsExactly(keyword, "new", "as", "n", "old", "o")
            val empty = p.parse("create trigger t before insert on tab $keyword for each row begin null; end;")
                .getFirstDescendant(PlSqlGrammar.REFERENCING_CLAUSE)
            assertThatAst(empty.texts()).containsExactly(keyword)
        }
    }
}
