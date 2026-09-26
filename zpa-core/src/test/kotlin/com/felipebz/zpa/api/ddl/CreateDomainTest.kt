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
        // A repeated property fails with ORA-00139/ORA-02258.
        assertThat(p).notMatches("create domain d as number display value display value")
        assertThat(p).notMatches("create domain d as number order value order value")
        assertThat(p).notMatches("create domain d as number default 1 default 2")
        assertThat(p).notMatches("create domain d as number annotations (A 'x') annotations (B 'y')")
        assertThat(p).notMatches("create domain d as number not null null")
        assertThat(p).notMatches("create domain d as json validate '{}' validate '{}'")
        // VALIDATE after a CHECK is its constraint state, so USING cannot follow (ORA-03049).
        assertThat(p).notMatches("create domain d as json constraint check (value is json) validate using '{}'")
        // CREATE-only annotation directives (ORA-11555/ORA-11556).
        assertThat(p).notMatches("create domain d as number annotations (drop A)")
        assertThat(p).notMatches("create domain d as number annotations (add or replace A 'x')")
    }

    @Test
    fun keepsEnumOutOfTheDatatypeBranch() {
        // Oracle 26 parses other names as datatypes (ORA-11531 afterwards) but always treats unquoted
        // ENUM as the enum branch (ORA-00904 right after a bare ENUM); that branch is not modeled yet.
        assertThat(p).matches("create domain d as some_unknown_type")
        assertThat(p).matches("create domain d as strict")
        assertThat(p).matches("create domain d as \"ENUM\"")
        assertThat(p).notMatches("create domain d as enum")
        assertThat(p).notMatches("create domain d as enum strict")
        assertThat(p).notMatches("create domain d as enum (a, b)")
    }
}
