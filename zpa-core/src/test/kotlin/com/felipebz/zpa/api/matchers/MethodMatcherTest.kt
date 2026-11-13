//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.matchers

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun keepsTheLastComparedComponentAfterSuccessfulMatch() {
        val matcher = MethodMatcher.create().schema("sch").packageName("pack").name("func")

        assertThat(matcher.matches(p.parse("sch.pack.func").firstChild)).isTrue
        assertThat(matcher.methodName).isEqualTo("SCH")
    }

    @Test
    fun keepsTheComponentThatFailedTheMethodComparison() {
        val matcher = MethodMatcher.create().name("func")

        assertThat(matcher.matches(p.parse("other").firstChild)).isFalse
        assertThat(matcher.methodName).isEqualTo("OTHER")
    }

    @Test
    fun keepsTheComponentThatFailedThePackageComparison() {
        val matcher = MethodMatcher.create().packageName("pack").name("func")

        assertThat(matcher.matches(p.parse("other.func").firstChild)).isFalse
        assertThat(matcher.methodName).isEqualTo("OTHER")
    }

    @Test
    fun keepsTheComponentThatFailedTheSchemaComparison() {
        val matcher = MethodMatcher.create().schema("sch").packageName("pack").name("func")

        assertThat(matcher.matches(p.parse("other.pack.func").firstChild)).isFalse
        assertThat(matcher.methodName).isEqualTo("OTHER")
    }

    @Test
    fun preservesTheFirstThreeComponentsWhenMoreArePresent() {
        val matcher = MethodMatcher.create().schema("a").packageName("b").name("c")

        assertThat(matcher.matches(p.parse("a.b.c.d").firstChild)).isTrue
        assertThat(matcher.methodName).isEqualTo("A")
    }

    @Test
    fun unwrapsCallStatement() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT)
        val matcher = MethodMatcher.create().name("func").withNoParameterConstraint()
        val callStatement = p.parse("begin func; end;").getFirstDescendant(PlSqlGrammar.CALL_STATEMENT)

        assertThat(matcher.matches(callStatement)).isTrue
    }

    @Test
    fun keepsTheMethodComponentWhenArgumentValidationFails() {
        val matcher = MethodMatcher.create().name("func").addParameter()

        assertThat(matcher.matches(p.parse("func(x, y)").firstChild)).isFalse
        assertThat(matcher.methodName).isEqualTo("FUNC")
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

    @Test
    @OptIn(ZpaExperimentalApi::class)
    fun nullLiteralPredicateMatchesNullAndTheEmptyString() {
        val matcher = MethodMatcher.create().name("nvl").addParameters(MethodMatcher.nullLiteral().negate(), MethodMatcher.nullLiteral())
        matches(matcher, "nvl(x, null)")
        matches(matcher, "nvl(x, '')")
        matches(matcher, "nvl(x, n'')")
        matches(matcher, "nvl(x, q'[]')")
        notMatches(matcher, "nvl(x, ' ')")
        notMatches(matcher, "nvl(x, y)")
        notMatches(matcher, "nvl(x, q'[ ]')")
        notMatches(matcher, "nvl(x, (null))")
        notMatches(matcher, "nvl(null, null)")
        // Named actuals: the predicate sees the expression after the arrow.
        matches(matcher, "nvl(expr1 => x, expr2 => null)")
        matches(matcher, "nvl(x, expr2 => '')")
        notMatches(matcher, "nvl(expr1 => x, expr2 => ' ')")
        notMatches(matcher, "nvl(expr1 => x, expr2 => y)")
        notMatches(matcher, "nvl(x)")
    }

    @Test
    @OptIn(ZpaExperimentalApi::class)
    fun predicateReceivesEachArgumentExpression() {
        val seen = mutableListOf<String>()
        val matcher = MethodMatcher.create().name("func")
            .addParameter { seen += it.tokens.joinToString(" ") { t -> t.originalValue }; true }
            .addParameter()
            .addParameter { it.type == PlSqlGrammar.LITERAL }
        matches(matcher, "func(a + 1, b, c => 3)")
        notMatches(matcher, "func(a, b, c => d)")
        assertThat(seen).containsExactly("a + 1", "a")
        assertThrows<IllegalStateException> {
            MethodMatcher.create().withNoParameterConstraint().addParameter(MethodMatcher.nullLiteral())
        }
    }

    fun matches(matcher: MethodMatcher, text: String) {
        assertThat(matcher.matches(p.parse(text).firstChild)).isTrue
    }

    fun notMatches(matcher: MethodMatcher, text: String) {
        assertThat(matcher.matches(p.parse(text).firstChild)).isFalse
    }

}
