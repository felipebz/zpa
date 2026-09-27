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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterTypeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ALTER_TYPE)
    }

    private fun assertMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).matches(source)
        }
    }

    private fun assertNotMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun matchesHeaderAndStandaloneActions() {
        assertMatches(
            "alter type cust_address_typ2 compile;",
            "alter type if exists hr.t compile;",
            "alter type t editionable;",
            "alter type t noneditionable",
            "alter type t reset;",
            "alter type t not final;",
            "alter type t final instantiable;",
            "alter type t not instantiable cascade;",
            "alter type t not final cascade including table data;"
        )
    }

    @Test
    fun matchesTypeCompileClause() {
        assertMatches(
            "alter type link2 compile specification;",
            "alter type link2 compile body;",
            "alter type t compile debug;",
            "alter type t compile debug body plsql_optimize_level = 2 reuse settings;",
            "alter type t compile plsql_optimize_level = 2 plsql_code_type = native;"
        )
        assertThatAst(p.parse("alter type t compile body;").getDescendants(DdlGrammar.TYPE_COMPILE_CLAUSE)).hasSize(1)
    }

    @Test
    fun rejectsInvalidHeaderAndCompileForms() {
        assertNotMatches(
            // ORA-03049
            "alter type t compile package;",
            "alter type t compile body debug;",
            "alter type t compile specification body;",
            "alter type t compile compile;",
            "alter type t compile cascade;",
            "alter type t editionable compile;",
            "alter type t editionable cascade;",
            // ORA-02000
            "alter type t compile reuse;",
            // ORA-11544
            "alter type if not exists t compile;",
            // ORA-00922
            "alter type a.b.t compile;",
            "alter type t@lnk compile;",
            "alter type t;",
            "alter type t reset cascade;"
        )
    }

    @Test
    fun matchesAttributeDefinitions() {
        assertMatches(
            "alter type textdoc_typ add attribute (author varchar2) cascade;",
            "alter type link1 add attribute (b number) invalidate;",
            "alter type t add attribute c number;",
            "alter type t add attribute (c number, d date) cascade not including table data;",
            "alter type t modify attribute b varchar2(20) cascade;",
            "alter type t modify attribute (b varchar2(20));",
            "alter type t drop attribute b cascade;",
            "alter type t drop attribute (b, c);",
            "alter type t add attribute (c number), add attribute (d number);",
            "alter type t add attribute (c number), modify attribute b varchar2(30), drop attribute e;",
            "alter type t add attribute (c sch.other_t) cascade;",
            // PLS-00218 and ORA-22344 are raised after parsing.
            "alter type t add attribute (c number not null);",
            "alter type t add attribute (c number) cascade convert to substitutable;"
        )
    }

    @Test
    fun rejectsInvalidAttributeDefinitions() {
        assertNotMatches(
            // PLS-00103 for each form.
            "alter type t add attribute c;",
            "alter type t add attribute (c);",
            "alter type t add attribute ();",
            "alter type t drop attribute b number;",
            "alter type t drop attribute (b number);",
            "alter type t add attribute (c number) add attribute (d number);",
            "alter type t add attribute (c number) modify attribute b varchar2(30);",
            "alter type t add attribute (c number), add member function g return number;"
        )
    }

    @Test
    fun matchesMethodSpecifications() {
        assertMatches(
            "alter type data_typ1 add member function qtr(der_qtr date) return char cascade;",
            "alter type t add static procedure p(x number);",
            "alter type t add member procedure p, add member function g return number cascade;",
            "alter type t drop member function f return number, add member function g return number invalidate;",
            "alter type t add map member function m return number;",
            "alter type t add order member function o(x t) return number;",
            "alter type t drop map member function m return number;",
            "alter type t add constructor function t(a number) return self as result;",
            "alter type t add final member function g return number;",
            "alter type t add not final member function g return number;",
            "alter type t add member function g return number deterministic parallel_enable;",
            "alter type t add member function g return number as language java name 'X.g() return int';",
            "alter type t add member function g return number, pragma deprecate(g), add member procedure p;"
        )
        val node = p.parse("alter type t add member function g return number, drop member procedure p;")
        assertThatAst(node.getDescendants(PlSqlGrammar.TYPE_ELEMENT_SPEC)).hasSize(2)
    }

    @Test
    fun rejectsInvalidMethodSpecifications() {
        assertNotMatches(
            // PLS-00103
            "alter type t add member procedure p add member procedure q;",
            "alter type t add function g return number;",
            "alter type t drop member function f;",
            "alter type t add member function g return number, add attribute (c number);",
            "alter type t add member function g return number pragma deprecate(g);",
            // ORA-00922
            "alter type t pragma deprecate(f);"
        )
    }

    @Test
    fun matchesCollectionClauses() {
        assertMatches(
            "alter type phone_list_typ_demo modify limit 10 cascade;",
            "alter type phone_list_typ modify element type varchar(64) cascade;",
            "alter type t modify limit 5 + 5;",
            "alter type t modify element type varchar2(64) not null;",
            "alter type t modify limit 10 cascade including table data;",
            "alter type t modify limit 10 cascade exceptions into sch.exc;",
            "alter type t modify limit 10 cascade not including table data force exceptions into exc;"
        )
    }

    @Test
    fun rejectsInvalidCollectionAndDependentClauses() {
        assertNotMatches(
            // PLS-00103 for each form.
            "alter type t modify limit 10, modify element type varchar2(64);",
            "alter type t modify limit 10 modify limit 12;",
            "alter type t modify limit 10 cascade force;",
            "alter type t add attribute (c number) invalidate cascade;",
            "alter type t add attribute (c number) invalidate force;",
            "alter type t add attribute (c number) cascade cascade;",
            "alter type t add attribute (c number) including table data;"
        )
    }

    @Test
    fun matchesReplaceClause() {
        assertMatches(
            "alter type t replace as object (a number, member function f return number);",
            "alter type t replace is object (a number);",
            "alter type t replace authid definer accessible by (x) as object (a number);",
            "alter type t replace accessible by (x) authid current_user as object (a number);",
            "alter type t replace force as object (a number) not final;",
            // ORA-02342 / ORA-22870 are raised after parsing.
            "alter type t replace under base_t (c number);",
            "alter type t replace as varray(5) of varchar2(10);"
        )
        assertNotMatches(
            // PLS-00103
            "alter type t replace as object (a number) cascade;",
            "alter type t replace sharing = none as object (a number);",
            "alter type t replace authid definer;"
        )
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThatAst(p.parse("alter type t compile;").getDescendants(PlSqlGrammar.ALTER_TYPE)).hasSize(1)
    }

}
