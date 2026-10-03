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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonOptionsTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonObjectWithTypename() {
        assertThat(p).matches("json_object(col1 with typename)")
        assertThat(p).matches("json_object(key 'a' value col1 with typename)")
        assertThat(p).matches("json_object('a' value x, 'b' value y with typename)")
        assertThat(p).matches("json_object(col1 format json with typename)")
        assertThat(p).matches("json_object(col1 null on null with typename)")
        assertThat(p).matches("json_object(col1 returning varchar2(10) with typename)")
        assertThat(p).matches("json_object(col1 null on null pretty ascii strict with typename with unique keys)")
        assertThat(p).matches("json_object(col1 with unique keys with typename)")
        assertThat(p).matches("json_object(* with typename)")
        assertThat(p).matches("json {col1 with typename}")
    }

    @Test
    fun rejectsMisplacedJsonObjectTypename() {
        assertThat(p).notMatches("json_object(col1 with typename, 'a' value 1)")
        assertThat(p).notMatches("json_object(col1 with typename format json)")
        assertThat(p).notMatches("json_object(col1 with typename null on null)")
        assertThat(p).notMatches("json_object(col1 with typename returning clob)")
        assertThat(p).notMatches("json_object(col1 with typename strict)")
        assertThat(p).notMatches("json_object(col1 with typename pretty)")
        assertThat(p).notMatches("json_object(col1 with)")
        assertThat(p).notMatches("json_object(col1 typename)")
        assertThat(p).notMatches("json_array(col1 with typename)")
    }

    @Test
    fun matchesJsonValueBooleanToNumberConversion() {
        assertThat(p).matches("json_value(d, '\$.a' returning number allow boolean to number conversion)")
        assertThat(p).matches("json_value(d, '\$.a' returning number disallow boolean to number conversion)")
        assertThat(p).matches("json_value(d, '\$.a' returning number(5, 2) allow boolean to number conversion)")
        assertThat(p).matches("json_value(d, '\$.a' returning number allow to number)")
        assertThat(p).matches("json_value(d, '\$.a' returning integer disallow boolean to number)")
        assertThat(p).matches("json_value(d, '\$.a' returning number allow boolean to number conversion null on error)")
        assertThat(p).matches("json_value(d, '\$.a' returning number allow boolean to number conversion type(lax))")
        assertThat(p).matches("json_value(d, '\$.a' returning binary_double)")
    }

    @Test
    fun rejectsInvalidBooleanToNumberConversion() {
        assertThat(p).notMatches("json_value(d, '\$.a' returning allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning varchar2(10) allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning date allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning boolean allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning binary_double allow boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning number boolean to number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning number allow boolean conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning number allow number conversion)")
        assertThat(p).notMatches("json_value(d, '\$.a' returning number allow boolean to number conversion allow boolean to number conversion)")
    }

    @Test
    fun matchesJsonSerializePresentationOptionsInAnyOrder() {
        assertThat(p).matches("json_serialize(d ascii pretty ordered)")
        assertThat(p).matches("json_serialize(d pretty ascii ordered)")
        assertThat(p).matches("json_serialize(d truncate pretty ascii)")
        assertThat(p).matches("json_serialize(d ascii truncate pretty ordered)")
        assertThat(p).matches("json_serialize(d truncate ordered)")
        assertThat(p).matches("json_serialize(d ordered truncate)")
        assertThat(p).matches("json_serialize(d truncate ordered truncate)")
        assertThat(p).matches("json_serialize(d pretty pretty)")
        assertThat(p).matches("json_serialize(d returning clob pretty ascii truncate ordered null on error)")
        assertThat(p).matches("json_serialize(d ordered error on error)")
        assertThat(p).matches("json_serialize(d pretty ascii ordered truncate empty object on error)")
    }

    @Test
    fun rejectsJsonSerializeOptionsAfterOrdered() {
        assertThat(p).notMatches("json_serialize(d ordered pretty)")
        assertThat(p).notMatches("json_serialize(d ordered ascii)")
        assertThat(p).notMatches("json_serialize(d ordered truncate pretty)")
        assertThat(p).notMatches("json_serialize(d ordered ordered)")
        assertThat(p).notMatches("json_serialize(d error on error pretty)")
        assertThat(p).notMatches("json_serialize(d ascii returning clob)")
        assertThat(p).notMatches("json_serialize(d pretty returning clob)")
        assertThat(p).notMatches("json_serialize(d ordered, pretty)")
    }

    @Test
    fun buildsJsonSerializeOptionTokens() {
        val tree = p.parse("json_serialize(d ascii truncate pretty ordered truncate)")
        val serialize = tree.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_SERIALIZE_EXPRESSION)!!
        assertThatAst(serialize.children.map { it.tokenOriginalValue.lowercase() })
            .containsExactly("json_serialize", "(", "d", "ascii", "truncate", "pretty", "ordered", "truncate", ")")
    }
}
