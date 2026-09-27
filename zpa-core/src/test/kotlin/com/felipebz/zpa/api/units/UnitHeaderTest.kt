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

class UnitHeaderTest : RuleTest() {

    private val procedureBody = "is begin null; end;"
    private val functionBody = "is begin return 1; end;"

    @Test
    fun matchesIfNotExistsOnProgramUnits() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        assertThat(p).matches("create procedure if not exists remove_emp (employee_id number) $procedureBody")
        assertThat(p).matches("create editionable procedure if not exists hr.p $procedureBody")

        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).matches("create noneditionable function if not exists f return number $functionBody")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE)
        assertThat(p).matches("create package if not exists emp_mgmt as procedure p; end emp_mgmt;")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY)
        assertThat(p).matches("create package body if not exists emp_mgmt as procedure p $procedureBody end;")
    }

    @Test
    fun rejectsInvalidIfNotExists() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        // ORA-11541
        assertThat(p).notMatches("create or replace procedure if not exists p $procedureBody")
        // ORA-11543
        assertThat(p).notMatches("create procedure if exists p $procedureBody")
        // PLS-00103: IF NOT EXISTS belongs before the name.
        assertThat(p).notMatches("create procedure p if not exists $procedureBody")

        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).notMatches("create or replace function if not exists f return number $functionBody")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE)
        assertThat(p).notMatches("create or replace package if not exists k as end;")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY)
        assertThat(p).notMatches("create or replace package body if not exists k as end;")
    }

    @Test
    fun matchesAccessorLists() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        assertThat(p).matches("create procedure p accessible by (api) $procedureBody")
        assertThat(p).matches(
            "create procedure p accessible by (api, package sch.pkg, procedure x, function y, trigger t, type ty) " +
                procedureBody
        )
        assertThat(p).matches("create procedure p accessible by (\"Api\", package \"S\".\"P\") $procedureBody")
        assertThat(p).matches("create procedure p accessible by (package package) $procedureBody")

        val node = p.parse("create procedure p accessible by (sch.api, type t) $procedureBody")
        assertThatAst(node.getDescendants(PlSqlGrammar.ACCESSIBLE_BY_CLAUSE)).hasSize(1)
    }

    @Test
    fun rejectsInvalidAccessorLists() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        // PLS-00103 for each form.
        assertThat(p).notMatches("create procedure p accessible by () $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by (api,) $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by api $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by (a.b.c) $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by (view v) $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by (package) $procedureBody")
        assertThat(p).notMatches("create procedure p accessible by (api@lnk) $procedureBody")
    }

    @Test
    fun matchesStandalonePropertiesInAnyOrder() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        assertThat(p).matches(
            "create or replace procedure prc_calendar_data (cal_cv in out calendar_data.calcurtyp, year_id int) " +
                "parallel_enable $procedureBody"
        )
        assertThat(p).matches(
            "create procedure p deterministic accessible by (a) parallel_enable authid current_user " +
                "default collation using_nls_comp shard_enable $procedureBody"
        )
        assertThat(p).matches("create procedure p sharing = none (x number) accessible by (a) authid definer $procedureBody")

        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).matches(
            "create function f return number deterministic accessible by (a) result_cache parallel_enable " +
                "authid definer shard_enable $functionBody"
        )
        assertThat(p).matches("create function f sharing = metadata (x number) return number accessible by (a) $functionBody")
    }

    @Test
    fun rejectsMisplacedStandaloneProperties() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        // PLS-00103: SHARING precedes the parameter list, and RESETTABLE is package-only.
        assertThat(p).notMatches("create procedure p (x number) sharing = none $procedureBody")
        assertThat(p).notMatches("create procedure p resettable $procedureBody")

        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).notMatches("create function f (x number) sharing = none return number $functionBody")
        assertThat(p).notMatches("create function f return number sharing = none $functionBody")
        assertThat(p).notMatches("create function f return number resettable $functionBody")
    }

    @Test
    fun matchesPipelinedClauses() {
        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).matches(
            "create function process_table(tab table) return table pipelined row polymorphic using process_table_pkg;"
        )
        assertThat(p).matches(
            "create function f(tab table, cols columns) return table pipelined table polymorphic using app.ptf_pkg;"
        )
        assertThat(p).matches("create function f(tab table) return table accessible by (x) pipelined row polymorphic using pkg;")
        assertThat(p).matches("create function f return numbers pipelined using impl_type;")
        assertThat(p).matches("create function f return numbers pipelined $functionBody")
        assertThat(p).matches("create function f(tab table) return table pipelined row polymorphic $functionBody")
    }

    @Test
    fun rejectsInvalidPipelinedClauses() {
        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        // PLS-00103: POLYMORPHIC needs ROW or TABLE, and USING ends the declaration.
        assertThat(p).notMatches("create function f(tab table) return table pipelined polymorphic using pkg;")
        assertThat(p).notMatches("create function f(tab table) return table pipelined row polymorphic using pkg $functionBody")
        assertThat(p).notMatches("create function f(tab table) return table pipelined row polymorphic using pkg deterministic;")
        assertThat(p).notMatches("create function f return numbers pipelined using $functionBody")
    }

    @Test
    fun matchesPackageHeaders() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE)
        assertThat(p).matches("create or replace package res_pkg resettable as function get_current_user return varchar2; end;")
        assertThat(p).matches("create package k sharing = metadata resettable authid definer accessible by (api) as end;")
        assertThat(p).matches("create package k accessible by (api) resettable default collation using_nls_comp as end;")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY)
        assertThat(p).matches("create or replace package body res_pkg resettable as name varchar2(50); end;")
        assertThat(p).matches("create package body k sharing = none resettable as end;")
        assertThat(p).matches("create package body k sharing = metadata is end;")
    }

    @Test
    fun rejectsInvalidPackageHeaders() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE)
        // PLS-00103 for each form.
        assertThat(p).notMatches("create package k resettable sharing = none as end;")
        assertThat(p).notMatches("create package k deterministic as end;")
        assertThat(p).notMatches("create package k parallel_enable as end;")

        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY)
        assertThat(p).notMatches("create package body k resettable sharing = none as end;")
        assertThat(p).notMatches("create package body k resettable resettable as end;")
        assertThat(p).notMatches("create package body k authid definer as end;")
        assertThat(p).notMatches("create package body k accessible by (x) as end;")
    }

    @Test
    fun matchesPackagedSubprogramProperties() {
        setRootRule(PlSqlGrammar.PROCEDURE_DECLARATION)
        assertThat(p).matches("procedure prc_with_properties parallel_enable accessible by (procedure access_prc);")
        assertThat(p).matches("procedure p accessible by (x) parallel_enable is begin null; end;")

        setRootRule(PlSqlGrammar.FUNCTION_DECLARATION)
        assertThat(p).matches("function f return number result_cache accessible by (x) deterministic;")
        assertThat(p).matches("function f return number deterministic accessible by (x) result_cache is begin return 1; end;")
        assertThat(p).matches("function f(tab table) return table pipelined row polymorphic using pkg;")
        assertThat(p).matches("function f(tab table) return table pipelined table polymorphic;")
    }

    @Test
    fun rejectsInvalidPackagedSubprogramProperties() {
        setRootRule(PlSqlGrammar.PROCEDURE_DECLARATION)
        // PLS-00103
        assertThat(p).notMatches("procedure p resettable;")
        assertThat(p).notMatches("procedure p accessible by ();")

        setRootRule(PlSqlGrammar.FUNCTION_DECLARATION)
        assertThat(p).notMatches("function f return number pipelined using pkg is begin return 1; end;")
    }

}
