/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.ConditionsGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class JsonExistsConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleJsonExists() {
        assertThat(p).matches("json_exists(doc, '$.a')")
    }

    @Test
    fun matchesJsonExistsFormatJson() {
        assertThat(p).matches("json_exists(doc format json, '$.a')")
    }

    @Test
    fun matchesJsonExistsWithPassing() {
        assertThat(p).matches("json_exists(doc, '$.a' passing var1 as a, var2 as b)")
    }

    @Test
    fun matchesJsonExistsOnError() {
        assertThat(p).matches("json_exists(doc, '$.a' error on error)")
    }

    @Test
    fun matchesJsonExistsStrict() {
        assertThat(p).matches("json_exists(doc, '$.a' type strict)")
    }

    @Test
    fun matchesJsonExistsOnEmpty() {
        assertThat(p).matches("json_exists(doc, '$.a' error on empty)")
    }

    @Test
    fun matchesLongJsonExists() {
        assertThat(p).matches("""
            json_exists(doc format json, '$.a'
            passing var1 as a, var2 as b
            false on error
            type lax
            true on empty)
            """)
    }

}
