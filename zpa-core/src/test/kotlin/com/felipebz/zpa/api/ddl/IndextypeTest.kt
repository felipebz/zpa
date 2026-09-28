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

class IndextypeTest : RuleTest() {

    @Test
    fun matchesBasicCreate() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create indextype position_indextype for position_between(number, number, number) using position_im;")
        assertThat(p).matches("create indextype hr.it for hr.op(varchar2) using hr.im")
        assertThat(p).matches("create indextype \"It X\" for \"OP\"(varchar2) using \"Im Type\"")
        assertThat(p).matches("create indextype it for op(varchar2), hr.op(number), op2(raw) using im")
    }

    @Test
    fun matchesCreateModifiers() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create or replace indextype it for op(varchar2) using im")
        assertThat(p).matches("create indextype if not exists it for op(varchar2) using im")
        assertThat(p).matches("create indextype it sharing = metadata for op(varchar2) using im")
        assertThat(p).matches("create indextype it sharing = data for op(varchar2) using im")
        assertThat(p).matches("create indextype if not exists it sharing = none for op(varchar2) using im")
    }

    @Test
    fun rejectsInvalidCreateHeader() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        // ORA-11541: OR REPLACE and IF NOT EXISTS cannot coexist.
        assertThat(p).notMatches("create or replace indextype if not exists it for op(varchar2) using im")
        assertThat(p).notMatches("create indextype it if not exists for op(varchar2) using im")
        assertThat(p).notMatches("create indextype if exists it for op(varchar2) using im")
        assertThat(p).notMatches("create indextype a.b.c for op(varchar2) using im")
        assertThat(p).notMatches("create indextype it@dbl for op(varchar2) using im")
        assertThat(p).notMatches("create indextype it sharing = extended data for op(varchar2) using im")
        assertThat(p).notMatches("create indextype sharing = none it for op(varchar2) using im")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im sharing = none")
        assertThat(p).notMatches("create indextype it")
        assertThat(p).notMatches("create indextype for op(varchar2) using im")
        assertThat(p).notMatches("create indextype it for op(varchar2)")
        assertThat(p).notMatches("create indextype it using im")
        assertThat(p).notMatches("create indextype it using im for op(varchar2)")
    }

    @Test
    fun rejectsMalformedOperatorSignatures() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).notMatches("create indextype it for op using im")
        assertThat(p).notMatches("create indextype it for op() using im")
        assertThat(p).notMatches("create indextype it for op(varchar2,) using im")
        assertThat(p).notMatches("create indextype it for op(varchar2), using im")
        assertThat(p).notMatches("create indextype it for op(varchar2), for op(number) using im")
        assertThat(p).notMatches("create indextype it for a.b.c(varchar2) using im")
        assertThat(p).notMatches("create indextype it for op@dbl(varchar2) using im")
    }

    @Test
    fun sharesOperatorParameterTypes() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create indextype it for op(long, long raw, double precision, double) using im")
        assertThat(p).matches("create indextype it for op(ot, hr.ot, sys.xmltype, timestamp, boolean) using im")
        assertThat(p).notMatches("create indextype it for op(varchar2(10)) using im")
        assertThat(p).notMatches("create indextype it for op(timestamp with time zone) using im")
        assertThat(p).notMatches("create indextype it for op(interval day to second) using im")
        assertThat(p).notMatches("create indextype it for op(national char) using im")
        assertThat(p).notMatches("create indextype it for op(ref ot) using im")
        assertThat(p).notMatches("create indextype it for op(a.b.ot) using im")
        assertThat(p).notMatches("create indextype it for op(1) using im")
    }

    @Test
    fun rejectsMalformedUsingType() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).notMatches("create indextype it for op(varchar2) using")
        assertThat(p).notMatches("create indextype it for op(varchar2) using a.b.c")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im@dbl")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im, im2")
    }

    @Test
    fun matchesArrayDml() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml")
        assertThat(p).matches("create indextype it for op(varchar2) using im without array dml")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (ot, vt)")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (hr.ot, hr.vt), (varchar2, \"VT\")")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (interval day to second, vt)")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (interval year to month, vt)")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (national char, vt)")
        assertThat(p).matches("create indextype it for op(varchar2) using im with array dml (double precision, vt)")
    }

    @Test
    fun rejectsMalformedArrayDml() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).notMatches("create indextype it for op(varchar2) using im array dml")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot, vt, x)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml ()")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot, vt) (ot2, vt2)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot, vt),")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (a.b.c, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot, a.b.c)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ot@dbl, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (number(5), vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (timestamp with time zone, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (char varying, vt)")
        // ORA-29892: REF, LONG and LONG RAW are rejected immediately.
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (ref ot, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (long, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml (long raw, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im without array dml (ot, vt)")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml with array dml")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with array dml without array dml")
    }

    @Test
    fun matchesPartitionAndStorage() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create indextype it for op(varchar2) using im with local partition")
        assertThat(p).matches("create indextype it for op(varchar2) using im with local range partition")
        assertThat(p).matches("create indextype it for op(varchar2) using im with system managed storage tables")
        assertThat(p).matches("create indextype it for op(varchar2) using im with user managed storage tables")
        assertThat(p).matches(
            "create indextype it for op(varchar2) using im with array dml with local range partition with user managed storage tables")
    }

    @Test
    fun matchesUsingOptionsInAnyOrder() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).matches("create indextype it for op(varchar2) using im with system managed storage tables with local partition")
        assertThat(p).matches("create indextype it for op(varchar2) using im with local partition with array dml")
        assertThat(p).matches("create indextype it for op(varchar2) using im with system managed storage tables with array dml")
        assertThat(p).matches(
            "create indextype it for op(varchar2) using im with local partition with system managed storage tables with array dml")
        assertThat(p).matches(
            "create indextype it for op(varchar2) using im " +
                "with system managed storage tables with array dml (ot, vt), (ot2, vt2) with local range partition")
    }

    @Test
    fun rejectsMalformedPartitionAndStorage() {
        setRootRule(DdlGrammar.CREATE_INDEXTYPE)
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with range local partition")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with local hash partition")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with local")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im local partition")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with local partition with local partition")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with system managed storage")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with managed storage tables")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with user managed storage tables tablespace users")
        assertThat(p).notMatches(
            "create indextype it for op(varchar2) using im with user managed storage tables with system managed storage tables")
        assertThat(p).notMatches(
            "create indextype it for op(varchar2) using im " +
                "with system managed storage tables with local partition with system managed storage tables")
        assertThat(p).notMatches("create indextype it for op(varchar2) using im with local range partition, with array dml")
        assertThat(p).notMatches("create indextype it for op(varchar2) with local partition using im")
        assertThat(p).notMatches("create indextype it for op(varchar2) with system managed storage tables using im")
    }

    @Test
    fun matchesAlterIndextype() {
        setRootRule(DdlGrammar.ALTER_INDEXTYPE)
        assertThat(p).matches("alter indextype position_indextype compile;")
        assertThat(p).matches("alter indextype if exists hr.it compile")
        assertThat(p).matches("alter indextype \"It X\" compile")
        assertThat(p).matches("alter indextype it add op(varchar2)")
        assertThat(p).matches("alter indextype it drop hr.op(varchar2)")
        assertThat(p).matches("alter indextype it add op(varchar2), drop op(number)")
        assertThat(p).matches("alter indextype it add op(varchar2) add op(number)")
        assertThat(p).matches("alter indextype it add op(varchar2), add op(number), add op(raw)")
        assertThat(p).matches("alter indextype it add op(varchar2), add op(number) add op(raw)")
        assertThat(p).matches("alter indextype it drop op(varchar2), drop op(number)")
        // A trailing comma after an ADD/DROP item is accepted.
        assertThat(p).matches("alter indextype it add op(varchar2),")
        assertThat(p).matches("alter indextype it add op(varchar2), drop op(number),")
        assertThat(p).matches("alter indextype it add op(varchar2), using im")
    }

    @Test
    fun matchesAlterUsingClause() {
        setRootRule(DdlGrammar.ALTER_INDEXTYPE)
        assertThat(p).matches("alter indextype it using im")
        assertThat(p).matches("alter indextype it using hr.im with array dml (ot, vt)")
        assertThat(p).matches("alter indextype it using im without array dml")
        assertThat(p).matches("alter indextype it add op(varchar2) using im with array dml")
        assertThat(p).matches("alter indextype it drop op(varchar2) using im")
        assertThat(p).matches("alter indextype it add op(varchar2) drop op(number) using im")
        assertThat(p).matches("alter indextype it add op(varchar2) using im with local range partition")
        assertThat(p).matches("alter indextype it add op(varchar2) using im with system managed storage tables with local partition")
        assertThat(p).matches("alter indextype it add op(varchar2) using im with local partition with array dml;")
    }

    @Test
    fun rejectsInvalidAlterIndextype() {
        setRootRule(DdlGrammar.ALTER_INDEXTYPE)
        assertThat(p).notMatches("alter indextype if not exists it compile")
        assertThat(p).notMatches("alter or replace indextype it compile")
        assertThat(p).notMatches("alter indextype a.b.c compile")
        assertThat(p).notMatches("alter indextype it@dbl compile")
        assertThat(p).notMatches("alter indextype it compile debug")
        assertThat(p).notMatches("alter indextype it compile reuse settings")
        assertThat(p).notMatches("alter indextype it compile compile")
        assertThat(p).notMatches("alter indextype it")
        assertThat(p).notMatches("alter indextype compile")
        assertThat(p).notMatches("alter indextype it sharing = none compile")
        assertThat(p).notMatches("alter indextype it compile with local partition")
        assertThat(p).notMatches("alter indextype it compile with user managed storage tables")
        assertThat(p).notMatches("alter indextype it compile add op(varchar2)")
        assertThat(p).notMatches("alter indextype it add op(varchar2) using im compile")
    }

    @Test
    fun rejectsMalformedAlterOperatorChanges() {
        setRootRule(DdlGrammar.ALTER_INDEXTYPE)
        assertThat(p).notMatches("alter indextype it add op(varchar2), op(number)")
        assertThat(p).notMatches("alter indextype it add op(varchar2),, add op(number)")
        assertThat(p).notMatches("alter indextype it , add op(varchar2)")
        // ADDs must precede DROPs (ORA-29841).
        assertThat(p).notMatches("alter indextype it drop op(varchar2), add op(number)")
        assertThat(p).notMatches("alter indextype it drop op(varchar2) add op(number)")
        assertThat(p).notMatches("alter indextype it add op(varchar2), drop op(number), add op(raw)")
        assertThat(p).notMatches("alter indextype it drop op(varchar2) force")
        assertThat(p).notMatches("alter indextype it add a.b.c(varchar2)")
        assertThat(p).notMatches("alter indextype it add op")
        assertThat(p).notMatches("alter indextype it add op()")
        assertThat(p).notMatches("alter indextype it add op(ref ot)")
        assertThat(p).notMatches("alter indextype it add op(varchar2(1))")
        assertThat(p).notMatches("alter indextype it add op(interval)")
        assertThat(p).notMatches("alter indextype it using im add op(varchar2)")
        assertThat(p).notMatches("alter indextype it using a.b.c")
        assertThat(p).notMatches("alter indextype it using im, im2")
        assertThat(p).notMatches("alter indextype it add op(varchar2) with local partition")
        assertThat(p).notMatches("alter indextype it with local partition")
        assertThat(p).notMatches("alter indextype it with system managed storage tables")
        assertThat(p).notMatches("alter indextype it with array dml")
        assertThat(p).notMatches("alter indextype it add op(varchar2) using im with local partition with local partition")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create indextype position_indextype for position_between(number, number, number) using position_im;")
        assertThat(p).matches("alter indextype position_indextype compile;")
        assertThat(p).matches("alter indextype it add op(varchar2) using im with local partition;")
    }
}
