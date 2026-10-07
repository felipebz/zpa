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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateDomainTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_DOMAIN)
    }

    @Test
    fun matchesRoot() {
        assertThat(p).matches("create domain d as number")
        assertThat(p).matches("create domain hr.d as varchar2(30);")
        assertThat(p).matches("create domain d as char(3 char) strict")
        assertThat(p).matches("create domain if not exists d as number")
        assertThat(p).matches("create usecase domain d as number")
        assertThat(p).matches("create usecase domain if not exists d as number")
        assertThat(p).matches("create domain d as json")
    }

    @Test
    fun matchesDefaultAndNullability() {
        assertThat(p).matches("create domain d as number default on null 15")
        assertThat(p).matches("create domain d as number default on null for insert only 1")
        assertThat(p).matches("create domain d as varchar2(30) default on null email_seq.nextval || '@domain.com'")
        assertThat(p).matches("create domain d as number not null")
        assertThat(p).matches("create domain d as number null default 1")
        assertThat(p).matches("create domain d as number default 1 not null")
    }

    @Test
    fun matchesConstraints() {
        assertThat(p).matches("create domain d as number constraint check (value >= 0)")
        assertThat(p).matches("create domain d as number check (value >= 0)")
        assertThat(p).matches("create domain d as number constraint c check (value >= 0) deferrable initially deferred")
        assertThat(p).matches("create domain d as number constraint check (value >= 0) initially deferred")
        assertThat(p).matches("create domain d as number constraint check (value >= 0) enable")
        assertThat(p).matches("create domain d as number constraint check (value >= 0) rely disable novalidate")
        assertThat(p).matches(
            "create domain d as number constraint c1 check (value >= 0) constraint c2 check (value <= 100) not null")
        assertThat(p).matches("create domain d as varchar2(12) constraint check (d not like '%[0-9]%') not null")
        assertThat(p).matches(
            "create domain d as char(3 char) constraint c check (upper(value) in ('MON', 'TUE')) " +
                "deferrable initially deferred display substr(value, 1, 2)")
    }

    @Test
    fun matchesJsonValidation() {
        assertThat(p).matches("create domain d as json validate '{\"type\" : \"object\"}'")
        assertThat(p).matches("create domain d as json validate using '{\"type\" : \"object\"}'")
        assertThat(p).matches("create domain d as json validate cast using '{\"type\" : \"object\"}'")
        assertThat(p).matches("create domain d as json constraint check (value is json validate using '{}')")
        assertThat(p).matches("create domain d as json constraint c check (d is json validate '{}')")
    }

    @Test
    fun matchesDisplayOrderAndAnnotations() {
        assertThat(p).matches("create domain d as number display to_char(value) order value annotations(Title 'D')")
        assertThat(p).matches(
            "create domain d as number(4) constraint check ((trunc(d) = d) and (d >= 1900)) " +
                "display (case when d < 2000 then '19-' else '20-' end) || mod(d, 100) order d - 1900")
        assertThat(p).matches(
            "create domain d as char(3 char) order case upper(d) when 'MON' then 0 else 7 end")
        assertThat(p).matches("create domain d as number(10) order ( -1*d ) annotations (Title 'Domain Annotation')")
        assertThat(p).matches("create domain d as number annotations(add if not exists A 'x')")
    }

    @Test
    fun matchesPropertiesInAnyOrder() {
        // Oracle 26 accepts the properties in any order after STRICT.
        assertThat(p).matches("create domain d as number order value display value")
        assertThat(p).matches("create domain d as number annotations (A 'x') display value")
        assertThat(p).matches("create domain d as number display value constraint check (value > 0)")
        assertThat(p).matches("create domain d as number constraint check (value >= 0) default 1")
        assertThat(p).matches("create domain d as number order value default 1 display value")
        assertThat(p).matches(
            "create domain d as number constraint c1 check (value > 0) default 1 " +
                "constraint c2 check (value < 9) display value check (value <> 5)")
        assertThat(p).matches("create domain d as json not null validate '{}'")
        assertThat(p).matches("create domain d as number strict default 1")
    }

    @Test
    fun rejectsMalformedRoot() {
        assertThat(p).notMatches("create domain")
        assertThat(p).notMatches("create domain d")
        assertThat(p).notMatches("create domain d as")
        assertThat(p).notMatches("create domain a.b.c as number")
        assertThat(p).notMatches("create domain d as number strict strict")
        assertThat(p).notMatches("create domain d as number default 1 strict")
        assertThat(p).notMatches("create domain d as number display value strict")
    }

    @Test
    fun rejectsMalformedProperties() {
        assertThat(p).notMatches("create domain d as number constraint")
        assertThat(p).notMatches("create domain d as number check")
        assertThat(p).notMatches("create domain d as number check ()")
        assertThat(p).notMatches("create domain d as number display")
        assertThat(p).notMatches("create domain d as number order")
        assertThat(p).notMatches("create domain d as number annotations()")
        assertThat(p).notMatches("create domain d as number default")
        assertThat(p).notMatches("create domain d as json validate")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // A name or state only applies to CHECK (ORA-02253/ORA-02250/ORA-03049).
        assertThat(p).notMatches("create domain d as number constraint c not null")
        assertThat(p).notMatches("create domain d as number constraint not null")
        assertThat(p).notMatches("create domain d as number not null enable")
        assertThat(p).notMatches("create domain d as number constraint check (value >= 0) using index")
        assertThat(p).notMatches("create domain d as number constraint check (value >= 0) precheck")
        assertThat(p).notMatches("create domain d as number constraint check (value >= 0) exceptions into ex")
        // VALIDATE after a CHECK is its constraint state, so USING cannot follow (ORA-03049).
        assertThat(p).notMatches("create domain d as json constraint check (value is json) validate using '{}'")
        // CREATE-only annotation directives (ORA-11555/ORA-11556).
        assertThat(p).notMatches("create domain d as number annotations (drop A)")
        assertThat(p).notMatches("create domain d as number annotations (add or replace A 'x')")
    }

    @Test
    fun acceptsRepeatedSingletonProperties() {
        // Oracle rejects these (ORA-00139/ORA-02258), but uniqueness is not tracked:
        // tracking that per property makes the compiled grammar grow factorially, so the parser
        // accepts repeats.
        assertThat(p).matches("create domain d as number display value display value")
        assertThat(p).matches("create domain d as number order value order value")
        assertThat(p).matches("create domain d as number default 1 default 2")
        assertThat(p).matches("create domain d as number annotations (A 'x') annotations (B 'y')")
        assertThat(p).matches("create domain d as number not null null")
        assertThat(p).matches("create domain d as json validate '{}' validate '{}'")
    }

    @Test
    fun matchesEnumDomain() {
        assertThat(p).matches("create domain order_status as enum (New, Open, Shipped, Closed, Cancelled);")
        assertThat(p).matches("create domain days_of_week as enum (Sunday = Su = 0, Monday = Mo, Tuesday = Tu);")
        assertThat(p).matches("create domain d as enum (a = 1 + 1, b = -3, c = 'x', e = date '2020-01-01', " +
            "f = to_number('1'), g = null, \"New\" = 5,)")
        assertThat(p).matches("create domain d as enum ()")
        assertThat(p).matches("create domain d as enum (a) strict not null default 1 display d order d annotations (x 'y')")
        assertThat(p).matches("create domain d as (c1 as enum (a = 1, b = 2) strict not null, c2 as number) check (c1 > 0)")
    }

    @Test
    fun rejectsMalformedEnum() {
        // Unquoted ENUM always starts the enum branch (ORA-00904 after a bare ENUM); "ENUM" is a datatype name.
        assertThat(p).matches("create domain d as some_unknown_type")
        assertThat(p).matches("create domain d as strict")
        assertThat(p).matches("create domain d as \"ENUM\"")
        assertThat(p).notMatches("create domain d as enum")
        assertThat(p).notMatches("create domain d as enum strict")
        // ORA-00917: items need commas, and the value must come after every alias.
        assertThat(p).notMatches("create domain d as enum (a b)")
        assertThat(p).notMatches("create domain d as enum (a = 1 = b)")
    }

    @Test
    fun matchesMultiColumnDomain() {
        assertThat(p).matches("create domain dgreater as (c1 as number, c2 as number) check (c1 > c2);")
        assertThat(p).matches("create usecase domain if not exists hr.d as (c1 as number strict, c2 as char(2) strict,)")
        assertThat(p).matches("create domain us_city as (" +
            "name as varchar2(30) annotations (Address), " +
            "state as varchar2(2) not null constraint st_c check (length(state) = 2) deferrable initially deferred, " +
            "zip as number default on null for insert only 1 collate binary) " +
            "constraint city_ck check (zip < 100000) " +
            "display name || ', ' || state " +
            "order state || name " +
            "annotations (Title 'Domain Annotation')")
        assertThat(p).matches("create domain d as (c1 as number check (c1 > 0) not null, c2 as json validate '{}') " +
            "order c1 display c1 constraint check (c1 < c2) rely annotations (a) check (c2 > 0)")
    }

    @Test
    fun rejectsMultiColumnPropertiesOutOfPlace() {
        // Columns reject DISPLAY and ORDER (ORA-00904/ORA-03050) and need AS (ORA-00904).
        assertThat(p).notMatches("create domain d as (c1 as number display c1)")
        assertThat(p).notMatches("create domain d as (c1 as number order c1)")
        assertThat(p).notMatches("create domain d as (c1 number)")
        assertThat(p).notMatches("create domain d as ()")
        // The domain as a whole rejects column-level properties (ORA-03048/ORA-03049).
        assertThat(p).notMatches("create domain d as (c1 as number) not null")
        assertThat(p).notMatches("create domain d as (c1 as number) default 1")
        assertThat(p).notMatches("create domain d as (c1 as number) strict")
        assertThat(p).notMatches("create domain d as (c1 as number) validate '{}'")
        assertThat(p).notMatches("create domain d as (c1 as number) collate binary")
    }

    @Test
    fun matchesMultiColumnDomainWithOptionalSeparators() {
        listOf(
            "create domain d as (a as number b as varchar2(20));",
            "create domain d as (a as number b as varchar2(20) c as date);",
            "create domain d as (a as number, b as varchar2(20) c as date);",
            "create domain d as (a as number b as varchar2(20), c as date);",
            "create domain d as (a as number b as varchar2(20),);",
            "create domain d as (a as number,);", "create domain d as (a as number);",
            "create domain d as (a as number b as number c as number d as number);",
            "create domain d as (a as number not null b as date default sysdate);",
            "create domain d as (a as number strict b as date strict);",
            "create domain d as (a as number default 1 b as date);",
            "create domain d as (a as number check (a > 0) b as date);",
            "create domain d as (a as number annotations (x 'y') b as date);",
            "create domain d as (a as number collate binary b as date);",
            "create domain d as (a as number b as date) display a order a check (a > 0);",
            "create domain d as (a as number b as date) constraint c check (a > 0) annotations (x 'y');",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesMultiColumnDocumentationExamples() {
        val columns = "(\n  amount        AS NUMBER(10, 2)\n  currency_code AS CHAR(3 CHAR)\n)"
        listOf(
            "CREATE DOMAIN currency AS $columns\nCONSTRAINT supported_currencies_c\n  CHECK ( currency_code IN ( 'USD', 'GBP', 'EUR', 'JPY' ) )\n" +
                "  DEFERRABLE INITIALLY DEFERRED\nCONSTRAINT non_negative_amounts_c\n  CHECK ( amount >= 0 )\n  DEFERRABLE INITIALLY DEFERRED;",
            "CREATE DOMAIN currency AS $columns\nDISPLAY CASE currency_code\n  WHEN 'USD' THEN '\$'\n  WHEN 'GBP' THEN '£'\n" +
                "  WHEN 'EUR' THEN '€'\n  WHEN 'JPY' THEN '¥'\nEND || TO_CHAR(amount, '999,999,999.00');",
            "CREATE DOMAIN co.currency AS $columns;",
            "CREATE DOMAIN currency AS $columns\nORDER currency_code || TO_CHAR(amount, '999999999.00');",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedMultiColumnSeparators() {
        listOf(
            "create domain d as (a as number,, b as date)", "create domain d as (,a as number, b as date)",
            "create domain d as (a as number b as date,,)", "create domain d as (a as number , , b as date)",
            "create domain d as (a as)", "create domain d as (as number, b as date)", "create domain d as (a number b date)",
            "create domain d as (a as number b)", "create domain d as ((a as number, b as date))",
            "create domain d as (a as number b as date) strict", "create domain d as (a as number b as date) default 1",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsMultiColumnDomainWithoutSeparatorNodes() {
        val tree = p.parse("create domain d as (a as number b as varchar2(20) not null, c as date)")
        val columns = tree.getDescendants(DdlGrammar.DOMAIN_COLUMN)
        assertThatAst(columns.map { it.firstChild.tokenOriginalValue }).containsExactly("a", "b", "c")
        assertThatAst(columns.map { it.children.map { c -> c.name } }).containsExactly(
            listOf("IDENTIFIER_NAME", "AS", "DATATYPE"),
            listOf("IDENTIFIER_NAME", "AS", "DATATYPE", "NOT", "NULL"),
            listOf("IDENTIFIER_NAME", "AS", "DATATYPE"))
        assertThatAst(tree.children.map { it.name }).doesNotContain("COMMA_WRAPPER")
        assertThatAst(p.parse("create domain d as number not null").children.map { it.name }).containsExactly(
            "CREATE", "DOMAIN", "IDENTIFIER_NAME", "AS", "DATATYPE", "NOT", "NULL")
    }
}
