/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.matchers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class MethodMatcherTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun detectSimpleMethodByName() {
        val matcher = MethodMatcher.create().name("func")
        matches(matcher, "func")
        notMatches(matcher, "foo")
        notMatches(matcher, "foo.func")
    }

    @Test
    fun detectAnyMethod() {
        val matcher = MethodMatcher.create().name(NameCriteria.any())
        matches(matcher, "func")
        matches(matcher, "foo")
    }

    @Test
    fun detectAnyMethodInList() {
        val matcher = MethodMatcher.create().name(NameCriteria.`in`("func", "foo"))
        matches(matcher, "func")
        matches(matcher, "FOO")
        notMatches(matcher, "bar")
    }

    @Test
    fun detectMethodInPackage() {
        val matcher = MethodMatcher.create().packageName("pack").name("func")
        matches(matcher, "pack.func")
        notMatches(matcher, "pkg.func")
        notMatches(matcher, "func")
    }

    @Test
    fun detectMethodInAnyPackage() {
        val matcher = MethodMatcher.create().packageName(NameCriteria.any()).name("func")
        matches(matcher, "pack.func")
        matches(matcher, "pkg.func")
    }

    @Test
    fun detectMethodInSchema() {
        val matcher = MethodMatcher.create().schema("sch").name("func")
        matches(matcher, "sch.func")
        notMatches(matcher, "bar.func")
        notMatches(matcher, "func")
    }

    @Test
    fun detectMethodInAnySchema() {
        val matcher = MethodMatcher.create().schema(NameCriteria.any()).name("func")
        matches(matcher, "sch.func")
        matches(matcher, "bar.func")
    }

    @Test
    fun detectMethodInPackageAndSchema() {
        val matcher = MethodMatcher.create().schema("sch").packageName("pack").name("func")
        matches(matcher, "sch.pack.func")
        notMatches(matcher, "bar.pack.func")
    }

    @Test
    fun detectMethodInPackageAndOptionalSchema() {
        val matcher = MethodMatcher.create().schema("sch").schemaIsOptional().packageName("pack").name("func")
        matches(matcher, "sch.pack.func")
        matches(matcher, "pack.func")
    }

    @Test
    fun detectMethodWithOneParameter() {
        val matcher = MethodMatcher.create().name("func").addParameter()
        matches(matcher, "func(x)")
        notMatches(matcher, "func(x, y)")
        notMatches(matcher, "bar.func(x)")
    }

    @Test
    fun detectMethodWithMoreParameters() {
        val matcher = MethodMatcher.create().name("func").addParameter().addParameter().addParameter()
        matches(matcher, "func(x, y, z)")
        notMatches(matcher, "func(x)")
    }

    @Test
    fun detectMethodWithMoreParameters2() {
        val matcher = MethodMatcher.create().name("func").addParameters(3)
        matches(matcher, "func(x, y, z)")
        notMatches(matcher, "func(x)")
    }

    @Test
    fun detectMethodInPackageWithParameters() {
        val matcher = MethodMatcher.create().packageName("pack").name("func").addParameter().addParameter().addParameter()
        matches(matcher, "pack.func(x, y, z)")
        notMatches(matcher, "bar.func(x)")
    }

    @Test
    fun detectMethodWithoutParameterConstraint() {
        val matcher = MethodMatcher.create().name("func").withNoParameterConstraint()
        matches(matcher, "func(x)")
    }

    @Test
    fun detectHostMethodCall() {
        val matcher = MethodMatcher.create().name("func").withNoParameterConstraint()
        matches(matcher, ":func(x)")
    }

    @Test
    fun shouldFailIfNameIsCalledMoreThanOnce() {
        val matcher = MethodMatcher.create().name("name")
        assertThrows<IllegalStateException> {
            matcher.name("name")
        }
    }

    @Test
    fun shouldFailIfPackageNameIsCalledMoreThanOnce() {
        val matcher = MethodMatcher.create().packageName("name")
        assertThrows<IllegalStateException> {
            matcher.packageName("name")
        }
    }

    @Test
    fun shouldFailIfSchemaIsCalledMoreThanOnce() {
        val matcher = MethodMatcher.create().schema("name")
        assertThrows<IllegalStateException> {
            matcher.schema("name")
        }
    }

    @Test
    fun shouldFailIfAddParameterIsCalledAfterWithNoParameterConstraint() {
        val matcher = MethodMatcher.create().withNoParameterConstraint()
        assertThrows<IllegalStateException> {
            matcher.addParameter()
        }
    }

    @Test
    fun shouldFailIfAddParametersIsCalledAfterWithNoParameterConstraint() {
        val matcher = MethodMatcher.create().withNoParameterConstraint()
        assertThrows<IllegalStateException> {
            matcher.addParameters(2)
        }
    }

    @Test
    fun shouldFailIfWithNoParameterConstraintIsCalledAfterAddParameter() {
        val matcher = MethodMatcher.create().addParameter()
        assertThrows<IllegalStateException> {
            matcher.withNoParameterConstraint()
        }
    }

    @Test
    fun shouldFailIfWithNoParameterConstraintIsCalledAfterAddParameters() {
        val matcher = MethodMatcher.create().addParameters(2)
        assertThrows<IllegalStateException> {
            matcher.withNoParameterConstraint()
        }
    }

    fun matches(matcher: MethodMatcher, text: String) {
        assertThat(matcher.matches(p.parse(text).firstChild)).isTrue
    }

    fun notMatches(matcher: MethodMatcher, text: String) {
        assertThat(matcher.matches(p.parse(text).firstChild)).isFalse
    }

}
