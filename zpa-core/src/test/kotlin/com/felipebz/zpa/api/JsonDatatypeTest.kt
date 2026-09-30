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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.symbols.datatype.JsonDatatype
import com.felipebz.zpa.symbols.DefaultTypeSolver
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonDatatypeTest : RuleTest() {

    private val scalarTypes = listOf(
        "number", "string", "binary_double", "binary_float", "date", "timestamp",
        "timestamp with time zone", "null", "boolean", "binary",
        "interval year to month", "interval day to second"
    )

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.JSON_DATATYPE)
    }

    @Test
    fun matchesTypeModifiersAndCombinations() {
        listOf(
            "json", "json (object)", "json (array)", "json (scalar)",
            "json object", "json array", "json scalar number",
            "json (object, array)", "json (object, scalar date)",
            "json (scalar number, scalar string)", "json (object, array, scalar)",
            "json (object, object)", "json (scalar, scalar number)",
            "json (scalar number, scalar number)", "json (array, array(number))",
            "json (object,)", "json (object,,array)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
        scalarTypes.forEach { scalar ->
            assertThat(p).matches("json (scalar $scalar)")
            assertThat(p).matches("json (array($scalar))")
        }
    }

    @Test
    fun matchesTypedArraysAndTheirOptions() {
        listOf(
            "json (array())", "json (array(allow null))", "json (array(,10))",
            "json (array(number allow null))", "json (array(number disallow null))",
            "json (array(number allow null disallow null))",
            "json (array(number, 10))", "json (array(number, *))",
            "json (array(number, *, sort))", "json (array(number, 10, sort))",
            "json (array(number allow null, *, sort))", "json (array(number, *,))",
            "json (array(number, 10,))", "json array(number, *,)", "json array(number, 10,)",
            "json (object, array(number, *, sort), scalar date)",
            "json array(timestamp with time zone, 10, sort)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun matchesByteLimitsWithoutEnforcingSemanticRestrictions() {
        listOf(
            "json (limit 500)", "json (object limit 500)", "json object limit 500",
            "json (object, array limit 500)", "json (object, limit 500)",
            "json (array(number) limit 500)", "json array(number) limit 500",
            // Oracle reports value/combination validation errors, not malformed productions.
            "json (limit 0)", "json (limit 33554433)",
            "json (array(number, 0))", "json (array(number, 10) limit 500)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun rejectsMalformedModifiersAndSeparateFeatures() {
        listOf(
            "json ()", "json (,object)", "json (unknown)", "json (object,unknown)",
            "json (object array)", "json (scalar varchar2)", "json (scalar number(5))",
            "json (scalar timestamp with local time zone)", "json (value)",
            "json (array(object))", "json (array(scalar number))", "json (array(unknown))",
            "json (array(number,))", "json (array(number,,sort))", "json (array(number,sort))",
            "json (array(number sort))", "json (array(number,*,sort,sort))",
            "json (array(number, *,, sort))", "json (array(number, 10,, sort))",
            "json array(number, *,, sort)", "json array(number, 10,, sort)",
            "json (array(number allow))", "json (array(number disallow null allow null))",
            "json (limit)", "json (limit *)", "json (limit maxsize)",
            "json (limit 1.5)", "json (limit 1+2)", "json (limit 10,object)",
            "json (object limit 10,array)", "json limit 500", "json (object) limit 500",
            "json validate '{}'", "json strict", "json with unique keys"
        ).forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun preservesJsonDatatypeAstAndTypeResolution() {
        setRootRule(PlSqlGrammar.DATATYPE)
        listOf("json", "json (object)", "json (scalar number)", "json (array(number,*,sort))").forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val datatype = p.parse(source)
            assertThatAst(datatype.type).isEqualTo(PlSqlGrammar.DATATYPE)
            assertThatAst(datatype.firstChild.type).isEqualTo(PlSqlGrammar.JSON_DATATYPE)
            assertThatAst(datatype.getDescendants(PlSqlGrammar.JSON_DATATYPE)).hasSize(1)
            assertThatAst(DefaultTypeSolver().solve(datatype, null)).isInstanceOf(JsonDatatype::class.java)
        }
    }

    @Test
    fun matchesSqlDatatypeContexts() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table jt (j json (object));")
        assertThat(p).matches("alter table jt add j2 json (array(number,*,sort));")
        assertThat(p).matches("create domain jd as json (object, scalar date limit 1000);")
        assertThat(p).matches("select cast(null as json (array(number,*,sort))) from dual;")
        assertThat(p).matches("declare j json; begin select cast(null as json (object)) into j from dual; end;")
    }

    @Test
    fun preservesModifierWordsAsIdentifiers() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table jt (object number, array number, scalar number);")
        assertThat(p).matches("select object as scalar, array as object, scalar as array from jt;")
        assertThat(p).matches("select object(), array(), scalar() from dual;")
        assertThat(p).matches("declare object number; array number; scalar number; begin null; end;")
    }
}
