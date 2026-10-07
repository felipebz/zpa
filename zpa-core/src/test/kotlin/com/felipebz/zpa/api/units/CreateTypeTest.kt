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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CreateTypeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_TYPE)
    }

    @Test
    fun matchesIncompleteType() {
        assertThat(p).matches("create type foo;")
    }

    @Test
    fun matchesSimpleCreateTypeObject() {
        assertThat(p).matches("create type foo as object (x number(5));")
    }

    @Test
    fun matchesCreateEditionableType() {
        assertThat(p).matches("create editionable type foo as object (x number(5));")
    }

    @Test
    fun matchesCreateNonEditionableType() {
        assertThat(p).matches("create noneditionable type foo as object (x number(5));")
    }

    @Test
    fun matchesCreateTypeWithSharingMetadata() {
        assertThat(p).matches("create type foo sharing = metadata as object (x number(5));")
    }

    @Test
    fun matchesCreateTypeWithSharingNone() {
        assertThat(p).matches("create type foo sharing = none as object (x number(5));")
    }

    @Test
    fun matchesCreateTypeWithDefaultCollation() {
        assertThat(p).matches("create type foo default collation using_nls_comp as object (x number(5));")
    }

    @Test
    fun matchesCreateTypeWithAccesibleBy() {
        assertThat(p).matches("create type foo accessible by (type other_type) as object (x number(5));")
    }

    @Test
    fun matchesCreateTypeForce() {
        assertThat(p).matches("create type foo force as object (x number(5));")
    }

    @Test
    fun matchesSimpleCreateTypeUnder() {
        assertThat(p).matches("create type foo under bar (x number(5));")
    }

    @Test
    fun matchesSimpleCreateOrReplaceTypeObject() {
        assertThat(p).matches("create or replace type foo as object (x number(5));")
    }

    @Test
    fun matchesSimpleCreateTypeVarray() {
        assertThat(p).matches("create type foo is varray(2) of number(5);")
    }

    @Test
    fun matchesSimpleCreateTypeNestedTable() {
        assertThat(p).matches("create type foo is table of bar;")
    }

    @Test
    fun matchesManyAttributes() {
        assertThat(p).matches("create type foo as object (x number(5), y bar, z baz);")
    }

    @Test
    fun matchesCreateTypeFinal() {
        assertThat(p).matches("create type foo as object (x number(5)) final;")
    }

    @Test
    fun matchesCreateTypeNotFinal() {
        assertThat(p).matches("create type foo as object (x number(5)) not final;")
    }

    @Test
    fun matchesCreateTypeInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) instantiable;")
    }

    @Test
    fun matchesCreateTypeNotInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) not instantiable;")
    }

    @Test
    fun matchesCreateTypeNotFinalNotInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) not final not instantiable;")
    }

    @Test
    fun matchesProcedureMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "member procedure bar);")
    }

    @Test
    fun matchesFunctionMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "member function bar return number);")
    }

    @Test
    fun matchesStaticProcedure() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "static procedure bar);")
    }

    @Test
    fun matchesStaticFunction() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "static function bar return number);")
    }

    @Test
    fun matchesSimpleConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "constructor function bar return self as result);")
    }

    @Test
    fun matchesFinalConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "final constructor function bar return self as result);")
    }

    @Test
    fun matchesInstantiableConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "instantiable constructor function bar return self as result);")
    }

    @Test
    fun matchesFinalInstantiableConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "final instantiable constructor function bar return self as result);")
    }

    @Test
    fun matchesSimpleConstructorWithParameters() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "constructor function bar(x number, y number) return self as result);")
    }

    @Test
    fun matchesSimpleConstructorWithExplicitSelf() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "constructor function bar(self in out foo, x number) return self as result);")
    }

    @Test
    fun matchesConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "final constructor function bar return self as result);")
    }

    @Test
    fun matchesMapMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "map member function bar return number);")
    }

    @Test
    fun matchesOrderMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                        + "x number(5),"
                        + "order member function bar return number);")
    }

    @Test
    fun deprecatedType() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "  pragma deprecate (foo),"
                + "x number(5),"
                + "member function bar return number);")
    }

    @Test
    fun deprecatedTypeWithComment() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "  pragma deprecate (foo, 'is deprecated'),"
                + "x number(5),"
                + "member function bar return number);")
    }

    @Test
    fun deprecatedTypeVariable() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "x number(5),"
                + "  pragma deprecate (x),"
                + "member function bar return number);")
    }

    @Test
    fun deprecatedTypeVariableWithComment() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "x number(5),"
                + "  pragma deprecate (x, 'is deprecated'),"
                + "member function bar return number);")
    }

    @Test
    fun deprecatedTypeProcedure() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "x number(5),"
                + "member function bar return number,"
                + "  pragma deprecate (bar)"
                + ");")
    }

    @Test
    fun deprecatedTypeProcedureWithComment() {
        assertThat(p).matches(
            "create or replace type foo as object ("
                + "x number(5),"
                + "member function bar return number,"
                + "  pragma deprecate (bar, 'is deprecated')"
                + ");")
    }

    @Test
    fun matchesIfNotExists() {
        listOf(
            "create type if not exists varr_int as varray(10) of (pls_integer) not persistable;",
            "create type if not exists t as object (a number);", "create type if not exists t as varray(10) of number;",
            "create type if not exists t as table of number;", "create type if not exists hr.t as object (a number) not final;",
            "create editionable type if not exists t as object (a number);",
            "create nonEditionable type if not exists t as object (a number);",
            "create or replace type if not exists t as object (a number);",
            "create type if not exists t sharing = none as object (a number);",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create type if exists t as object (a number);", "create type if not t as object (a number);",
            "create type editionable if not exists t as object (a number);", "create type t if not exists as object (a number);",
            "create type if t as object (a number);",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesPersistableClauses() {
        listOf(
            "create type t as object (a number) persistable;", "create type t as object (a number) not persistable;",
            "create type t as object (a number) not final not persistable;", "create type t as object (a number) not persistable not final;",
            "create type t as object (a number) not instantiable not final persistable;",
            "create type t2 under t (b number) not persistable;",
            "create type t as varray(10) of (number) persistable;", "create type t as varray(10) of (number) not persistable;",
            "create type t as table of (number) persistable;", "create type t as table of (pls_integer) not persistable;",
            "create type t as varray(10) of (simple_integer) not persistable;", "create type t as varray(10) of (pls_integer);",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create type t as varray(10) of number persistable;", "create type t as varray(10) of number not persistable;",
            "create type t as table of number persistable;", "create type t as table of number not persistable;",
            "create type t as varray(10) of pls_integer not persistable;", "create type t as table of pls_integer not persistable;",
            "create type t as table of (number) not null;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesParenthesizedCollectionElements() {
        listOf(
            "create type t as varray(10) of (number);", "create type t as varray(10) of number;",
            "create type t as varray(10) of (varchar2(20));", "create type t as varray(10) of varchar2(20);",
            "create type t as table of (number);", "create type t as table of (varchar2(20));",
            "create type t as table of (number not null);", "create type t as table of number not null;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create type t as varray(10) of ();", 
            "create type t as varray(10) of (pls_integer;", "create type t as varray(10) of pls_integer);",
            "create type t as table of ();", "create type t as table of (number;",
            "create type t as table of number);",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsIfNotExistsAndPersistableWithoutWrapperNodes() {
        val tree = p.parse("create type if not exists varr_int as varray(10) of (pls_integer) not persistable;")
        assertThatAst(tree.children.map { it.name }.take(6)).containsExactly("CREATE", "TYPE", "IF", "NOT", "EXISTS", "UNIT_NAME")
        assertThatAst(tree.getFirstDescendant(PlSqlGrammar.DATATYPE).tokens.map { it.originalValue }).containsExactly("pls_integer")
        val persistable = tree.getFirstDescendant(PlSqlGrammar.PERSISTABLE_CLAUSE)
        assertThatAst(persistable.children.map { it.name }).containsExactly("NOT", "PERSISTABLE")
        assertThatAst(p.parse("create type foo as object (x number)").children.map { it.name }.take(4))
            .containsExactly("CREATE", "TYPE", "UNIT_NAME", "OBJECT_TYPE_DEFINITION")
    }
}
