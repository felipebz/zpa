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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class ObjectTypeAttributesTest : RuleTest() {

    private val oid = "oid '82A4AF6A4CD4656DE034080020E0EE3D'"

    private fun matchesType(vararg statements: String) {
        setRootRule(PlSqlGrammar.CREATE_TYPE)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notMatchesType(vararg statements: String) {
        setRootRule(PlSqlGrammar.CREATE_TYPE)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    private fun matchesView(vararg statements: String) {
        setRootRule(PlSqlGrammar.CREATE_VIEW)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notMatchesView(vararg statements: String) {
        setRootRule(PlSqlGrammar.CREATE_VIEW)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    @Test
    fun matchesTypeOid() {
        matchesType(
            "create type t $oid as object (a number)", "create or replace type t $oid as object (a number);",
            "create editionable type t $oid as object (a number)",
            "create type t $oid as table of number", "create type t $oid as varray(3) of number",
            "create type t $oid as table of (number)", "create type t $oid under s (b number)",
            "create type t $oid", "create type t $oid authid current_user as object (a number)",
            "create type t sharing = none $oid as object (a number)", "create type t sharing = none force $oid as object (a number)",
            "create type t sharing = none $oid force as object (a number)", "create type t force $oid as object (a number)",
            "create type t $oid force as object (a number)", "create type t oid q'[AB]' as object (a number)",
            "create type t oid N'AB' as object (a number)", "create type t oid 'x' as object (a number)", "create type t oid '' as object (a number)"
        )
    }

    @Test
    fun rejectsMisplacedOrMalformedTypeOid() {
        notMatchesType(
            "create type t authid current_user $oid as object (a number)", "create type t $oid sharing = none as object (a number)",
            "create type t sharing = none $oid sharing = none as object (a number)", "create type t $oid $oid as object (a number)",
            "create type t as object (a number) $oid", "create type t oid 123 as object (a number)", "create type t oid x as object (a number)",
            "create type t oid as object (a number)", "create type t force $oid force as object (a number)",
            "create type t $oid force force as object (a number)", "create type t force force as object (a number)",
            "create type t force sharing = none as object (a number)"
        )
    }

    @Test
    fun matchesPersistableObjectTypes() {
        matchesType(
            "create type t as object (a number) persistable", "create type t as object (a number) not persistable",
            "create type t as object (i pls_integer) not persistable", "create type t as object (a number) not final not persistable",
            "create type t as object (a number) not persistable not final", "create type t as object (a number) persistable instantiable",
            "create type t as object (a number) persistable not final not instantiable", "create type t $oid as object (a number) not persistable",
            "create type t under s (b number) not persistable", "create type t is object (a number) not persistable;",
            "create type t as object (a number) persistable persistable"
        )
        notMatchesType(
            "create type t as object (a number) not not persistable", "create type t as object (a number) not",
            "create type t as object (a number) persistables", "create type t as object (a number) persistable not"
        )
    }

    @Test
    fun matchesParenthesisedCollectionElements() {
        matchesType(
            "create type t as table of (number) not persistable", "create type t as table of (number) persistable",
            "create type t as table of (pls_integer) not persistable", "create type t as table of (number)",
            "create type t as table of (number not null)", "create type t as table of (number not null) not persistable",
            "create type t as table of (varchar2(10)) not persistable", "create type t is table of (number) not persistable",
            "create type t as varray(3) of (number) not persistable", "create type t as varray(3) of (number) persistable",
            "create type t as varray(3) of (number)", "create type t as varray(3) of (number not null) not persistable",
            "create type t as varying array(3) of (number) not persistable", "create type t as table of (number) not persistable;",
            "create type t as table of number", "create type t as varray(3) of number", "create type t as table of number not null"
        )
        notMatchesType(
            "create type t as table of number persistable", "create type t as table of number not persistable",
            "create type t as varray(3) of number not persistable", "create type t as varray(3) of number not null not persistable",
            "create type t as table of number not null not persistable", "create type t as table of ()", "create type t as table of (number) not final",
            "create type t as table of (number) not persistable not persistable", "create type t as table of (number) not not persistable"
        )
    }

    @Test
    fun matchesObjectViewOidSpellings() {
        for (spelling in listOf("oid", "identifier", "id")) {
            matchesView(
                "create or replace view v of t with object $spelling (a) as select a, b from x",
                "create or replace view v of t with object $spelling default as select a, b from x",
                "create or replace view v of t with object $spelling (a, b) as select a, b from x",
                "create or replace view v of s.t with object $spelling (a) as select a, b from x"
            )
        }
        matchesView(
            "create or replace view v of t with object oid (a + 1) as select a, b from x", "create or replace view v of t with object oid (1) as select a, b from x",
            "create or replace force view v of t with object oid (a) as select a, b from x",
            "create or replace view v of t with object oid (a) bequeath definer as select a, b from x",
            "create or replace view v of t with object oid (a) as select a, b from x with read only",
            "create or replace view v of t with object oid (a) (constraint c1 primary key (a) disable novalidate) as select a, b from x",
            "create or replace view v of t with object oid default (a primary key disable novalidate) as select a, b from x",
            "create or replace view v of t under sv as select a, b from x", "create or replace view v of xmltype with object oid default as select 1 from x",
            "create or replace view v of xmltype with object id default as select 1 from x", "create or replace view v of t with object identifier (a) as select a, b from x"
        )
    }

    @Test
    fun rejectsMalformedObjectViews() {
        notMatchesView(
            "create or replace view v of t with object oid () as select a, b from x", "create or replace view v of t with object oid a as select a, b from x",
            "create or replace view v of t with object oid as select a, b from x", "create or replace view v of t with object (a) as select a, b from x",
            "create or replace view v of t with oid (a) as select a, b from x", "create or replace view v of t with object oid (a) () as select a, b from x",
            "create or replace view v of t with object oid (a) with object oid (b) as select a, b from x",
            "create or replace view v of t with object oid (a) with check option as select a, b from x",
            "create or replace view v of t with object oid (a)",
            "create or replace view v of t with object oids (a) as select a, b from x"
        )
    }

    @Test
    fun keepsOidUsableAsIdentifier() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table oid_t (oid number, persistable number); select oid, persistable from oid_t;")
        assertThat(p).matches("declare oid number; persistable number; begin oid := persistable; end;\n/\n")
    }

    @Test
    fun buildsDedicatedNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "create type a as object (i pls_integer) not persistable;\n" +
                "create type b as table of (pls_integer) persistable;\n" +
                "create type c $oid as object (x number) not final;\n")
        assertThatAst(tree.getDescendants(PlSqlGrammar.CREATE_TYPE)).hasSize(3)
        val clauses = tree.getDescendants(PlSqlGrammar.PERSISTABLE_CLAUSE)
        assertThatAst(clauses).hasSize(2)
        assertThatAst(clauses.map { c -> c.children.map { it.tokenOriginalValue.lowercase() } }).containsExactly(
            listOf("not", "persistable"), listOf("persistable"))
        assertThatAst(tree.getDescendants(PlSqlGrammar.OBJECT_TYPE_DEFINITION)).hasSize(2)
    }
}
