/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class JsonArrayTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonArrayWithOneElement() {
        assertThat(p).matches("json_array(1)")
    }

    @Test
    fun matchesJsonArrayWithManyElement() {
        assertThat(p).matches("json_array(1, 'test', sysdate)")
    }

    @Test
    fun matchesJsonArrayWithFormat() {
        assertThat(p).matches("json_array('{}' format json)")
    }

    @Test
    fun matchesJsonArrayWithNullOnNull() {
        assertThat(p).matches("json_array(foo null on null)")
    }

    @Test
    fun matchesJsonArrayWithAbsentOnNull() {
        assertThat(p).matches("json_array(foo absent on null)")
    }

    @Test
    fun matchesJsonArrayWithReturning() {
        assertThat(p).matches("json_array(foo returning json)")
    }

    @Test
    fun matchesJsonArrayWithStrict() {
        assertThat(p).matches("json_array(foo strict)")
    }

    @Test
    fun matchesLongJsonArray() {
        assertThat(p).matches("json_array(json_array(1,2,3), 100, 'test', null null on null returning json strict)")
    }

    @Test
    fun matchesJsonArrayFromQuery() {
        /*
         * This syntax is listed on the JSON_ARRAY docs for Oracle 23ai:
         * https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_ARRAY.html
         *
         * However, according to the table "Table C-2 Oracle Support for Optional Features of SQL/Foundation"
         * it isn't supported yet:
         *   T811, Basic SQL/JSON constructor functions
         *   Oracle fully supports this feature, except for the JSON_ARRAY constructor by query.
         * https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Oracle-Support-for-Optional-Features-of-SQLFoundation2011.html.
         */
        assertThat(p).matches("json_array(select * from tab null on null returning json strict)")
    }

}
