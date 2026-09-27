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
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class OperatorTest : RuleTest() {

    @Test
    fun matchesBasicCreate() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator eq_op binding (varchar2, varchar2) return number using eq_f;")
        assertThat(p).matches("create operator eq_op binding (varchar2) return number using eq_f")
        assertThat(p).matches("create operator hr.eq_op binding (varchar2, varchar2) return number using eq_f")
        assertThat(p).matches("create operator \"Eq Op\" binding (varchar2, varchar2) return number using \"F\"")
    }

    @Test
    fun matchesCreateModifiers() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create or replace operator eq_op binding (varchar2) return number using f")
        assertThat(p).matches("create operator if not exists eq_op binding (varchar2) return number using f")
        assertThat(p).matches("create operator eq_op sharing = metadata binding (varchar2) return number using f")
        assertThat(p).matches("create operator eq_op sharing = data binding (varchar2) return number using f")
        assertThat(p).matches("create operator if not exists eq_op sharing = none binding (varchar2) return number using f")
    }

    @Test
    fun rejectsInvalidCreateModifiers() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        // ORA-11541: OR REPLACE and IF NOT EXISTS cannot coexist.
        assertThat(p).notMatches("create or replace operator if not exists eq_op binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op if not exists binding (varchar2) return number using f")
        assertThat(p).notMatches("create if not exists operator eq_op binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator if exists eq_op binding (varchar2) return number using f")
        assertThat(p).notMatches("create editionable operator eq_op binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator sharing = none eq_op binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op sharing none binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op sharing = extended data binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op sharing = none sharing = none binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op binding (varchar2) return number using f sharing = none")
        assertThat(p).notMatches("create operator a.b.c binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op@dbl binding (varchar2) return number using f")
        assertThat(p).notMatches("create operator eq_op")
        assertThat(p).notMatches("create operator binding (varchar2) return number using f")
    }

    @Test
    fun matchesMultipleBindings() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator op binding (varchar2, varchar2) return number using f, (number) return number using g")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number using f, (number) return number using g, " +
                "(raw) return number with column context using g")
    }

    @Test
    fun rejectsMalformedBindingLists() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f, binding (number) return number using g")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f binding (number) return number using g")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f,")
        assertThat(p).notMatches("create operator op binding () return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2,) return number using f")
        assertThat(p).notMatches("create operator op binding varchar2 return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2 varchar2) return number using f")
        assertThat(p).notMatches("create operator op binding (1) return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number")
        assertThat(p).notMatches("create operator op binding (varchar2) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return (number) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using")
    }

    @Test
    fun matchesBindingDatatypes() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator op binding (timestamp, raw, boolean, vector, json) return number using f")
        assertThat(p).matches("create operator op binding (double precision, long raw, long, double) return number using f")
        assertThat(p).matches("create operator op binding (ot, hr.ot, sys.odcinumberlist, \"VARCHAR2\") return hr.ot using f")
        assertThat(p).matches("create operator op binding (varchar2) return long raw using f")
        assertThat(p).matches("create operator op binding (varchar2) return double precision using f")
    }

    @Test
    fun rejectsBindingDatatypeModifiers() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).notMatches("create operator op binding (varchar2(10)) return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number(5) using f")
        assertThat(p).notMatches("create operator op binding (varchar2 char) return number using f")
        assertThat(p).notMatches("create operator op binding (timestamp(6)) return number using f")
        assertThat(p).notMatches("create operator op binding (timestamp with time zone) return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return timestamp with time zone using f")
        assertThat(p).notMatches("create operator op binding (char varying) return number using f")
        assertThat(p).notMatches("create operator op binding (interval day to second) return number using f")
        assertThat(p).notMatches("create operator op binding (interval) return number using f")
        assertThat(p).notMatches("create operator op binding (national char) return number using f")
        assertThat(p).notMatches("create operator op binding (t.c%type) return number using f")
        assertThat(p).notMatches("create operator op binding (ot%rowtype) return number using f")
        assertThat(p).notMatches("create operator op binding (a.b.ot) return number using f")
        assertThat(p).notMatches("create operator op binding (ot@dbl) return number using f")
        // ORA-29834: REF is rejected during parsing.
        assertThat(p).notMatches("create operator op binding (ref ot) return number using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return ref ot using f")
    }

    @Test
    fun matchesContextClauses() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator op binding (varchar2) return number with index context, scan context ot using f")
        assertThat(p).matches("create operator op binding (varchar2) return number with index context, scan context hr.\"OT\" using f")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number " +
                "with index context, scan context ot compute ancillary data using f")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number " +
                "with index context, scan context ot compute ancillary data with column context using f")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number with index context, scan context ot with column context using f")
        assertThat(p).matches("create operator op binding (varchar2) return number with column context using f")
    }

    @Test
    fun rejectsMalformedContextClauses() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context scan context ot using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context, using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context, scan context using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context, scan context a.b.c using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with index context, scan context ot@dbl using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number with index context, scan context ot compute ancillary using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number " +
                "with index context, scan context ot compute ancillary data compute ancillary data using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number with index context, scan context ot, scan context ot using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number compute ancillary data using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number scan context ot using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number with column context with index context, scan context ot using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with column context, scan context ot using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with column context with column context using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number with column using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f with column context")
    }

    @Test
    fun matchesAncillaryClauses() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator op binding (varchar2) return number ancillary to prim(varchar2, varchar2) using f")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number ancillary to prim (varchar2), hr.prim(number) using f")
        // Oracle 26 parses WITH COLUMN CONTEXT before ANCILLARY TO, contrary to the diagram.
        assertThat(p).matches(
            "create operator op binding (varchar2) return number with column context ancillary to prim(varchar2) using f")
        assertThat(p).matches(
            "create operator op binding (varchar2) return number using f, (number) return number ancillary to prim(number) using g")
    }

    @Test
    fun rejectsMalformedAncillaryClauses() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim() using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim(varchar2(1)) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim(ref ot) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to a.b.c(varchar2) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim@dbl(varchar2) using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary to prim(varchar2), using f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number ancillary prim(varchar2) using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number ancillary to prim(varchar2) with column context using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number ancillary to prim(varchar2) ancillary to prim(varchar2) using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number " +
                "with index context, scan context ot ancillary to prim(varchar2) using f")
        assertThat(p).notMatches(
            "create operator op binding (varchar2) return number " +
                "with index context, scan context ot with column context ancillary to prim(varchar2) using f")
    }

    @Test
    fun matchesUsingFunctionQualification() {
        setRootRule(DdlGrammar.CREATE_OPERATOR)
        assertThat(p).matches("create operator op binding (varchar2) return number using hr.f")
        assertThat(p).matches("create operator op binding (varchar2) return number using pk.f")
        assertThat(p).matches("create operator op binding (varchar2) return number using hr.pk.f")
        assertThat(p).matches("create operator op binding (varchar2) return number using hr.ot.f")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using a.b.c.d")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f@dbl")
        assertThat(p).notMatches("create operator op binding (varchar2) return number using f(a)")
    }

    @Test
    fun matchesAlterOperator() {
        setRootRule(DdlGrammar.ALTER_OPERATOR)
        assertThat(p).matches("alter operator eq_op compile;")
        assertThat(p).matches("alter operator if exists hr.eq_op compile")
        assertThat(p).matches("alter operator \"EQ_OP\" compile")
        assertThat(p).matches("alter operator eq_op add binding (number) return number using g")
        assertThat(p).matches("alter operator eq_op add binding (number) return number with column context using hr.pk.g")
        assertThat(p).matches(
            "alter operator eq_op add binding (number) return number " +
                "with index context, scan context ot compute ancillary data using g")
        assertThat(p).matches("alter operator eq_op add binding (number) return number ancillary to prim(varchar2) using g")
        assertThat(p).matches("alter operator eq_op drop binding (varchar2, varchar2)")
        assertThat(p).matches("alter operator eq_op drop binding (hr.ot) force;")
    }

    @Test
    fun rejectsInvalidAlterOperator() {
        setRootRule(DdlGrammar.ALTER_OPERATOR)
        assertThat(p).notMatches("alter operator a.b.c compile")
        assertThat(p).notMatches("alter operator eq_op@dbl compile")
        assertThat(p).notMatches("alter operator if not exists eq_op compile")
        assertThat(p).notMatches("alter or replace operator eq_op compile")
        assertThat(p).notMatches("alter operator eq_op")
        assertThat(p).notMatches("alter operator compile")
        assertThat(p).notMatches("alter operator eq_op compile compile")
        assertThat(p).notMatches("alter operator eq_op compile reuse settings")
        assertThat(p).notMatches("alter operator eq_op compile debug")
        assertThat(p).notMatches("alter operator eq_op sharing = none compile")
        assertThat(p).notMatches("alter operator eq_op add binding (number) return (number) using g")
        assertThat(p).notMatches("alter operator eq_op add binding (number) return number using g, (raw) return number using g")
        assertThat(p).notMatches("alter operator eq_op add binding (number) return number")
        assertThat(p).notMatches("alter operator eq_op add (number) return number using g")
        assertThat(p).notMatches("alter operator eq_op add binding (number(5)) return number using g")
        assertThat(p).notMatches("alter operator eq_op drop binding force (varchar2)")
        assertThat(p).notMatches("alter operator eq_op drop binding ()")
        assertThat(p).notMatches("alter operator eq_op drop binding")
        assertThat(p).notMatches("alter operator eq_op drop (varchar2)")
        assertThat(p).notMatches("alter operator eq_op drop binding (varchar2) force force")
        assertThat(p).notMatches("alter operator eq_op drop binding (varchar2), (number)")
        assertThat(p).notMatches("alter operator eq_op drop binding (ref ot)")
        assertThat(p).notMatches("alter operator eq_op compile add binding (number) return number using g")
        assertThat(p).notMatches("alter operator eq_op add binding (number) return number using g compile")
        assertThat(p).notMatches("alter operator eq_op drop binding (varchar2) compile")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create operator eq_op binding (varchar2, varchar2) return number using eq_f;")
        assertThat(p).matches("alter operator eq_op compile;")
        assertThat(p).matches("alter operator eq_op drop binding (varchar2) force;")
    }
}
