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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.symbols.local.LocalReferenceContext.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Object types and `SELF` on source shapes observed on Oracle. Each rule is checked as written, with conditional
 * compilation in a region the outcome relies on, and in a region it does not rely on.
 */
class LocalObjectTypeResolverTest {

    private val support = LocalQualifiedResolverTest()

    private fun steps(source: String, name: String, occurrence: Int = 0, context: LocalReferenceContext? = null) =
        support.steps(source, name, occurrence, context)

    private fun assertSteps(template: String, name: String, expected: List<String>, occurrence: Int = 0, context: LocalReferenceContext? = null) =
        support.assertSteps(template, name, expected, occurrence, context)

    private val other = "create procedure other is begin /*S*/ null; end;\n/\n"

    @Test
    fun aMemberMethodSeesAttributesAndMethodsUnqualified() {
        val source = "create type unqualified_members as object (\n  x number,\n  member function h return number,\n  member function g return number\n);\n/\n" +
            "create type body unqualified_members as\n  member function h return number is begin return 1; end;\n" +
            "  member function g return number is begin /*A*/ return x + h(); end;\nend;\n/\n" + other
        assertSteps(source, "x", listOf("Bound(ATTRIBUTE x@2)"))
        assertSteps(source, "h", listOf("OverloadSet(FUNCTION h@8)"), context = CALL_EXPRESSION)
        // A member procedure assigns an attribute: the mode of SELF is typing.
        assertThat(steps("create type assigned as object (x number, member procedure p);\n/\n" +
            "create type body assigned as\n  member procedure p is begin x := 7; end;\nend;\n/\n", "x"))
            .isEqualTo("Bound(ATTRIBUTE x@1)")
    }

    @Test
    fun parametersAndLocalsHideAttributesAndSelfReachesThem() {
        val spec = "create type hidden_attribute as object (\n  x number,\n  member function fp(x number) return number,\n" +
            "  member function fl return number,\n  member function fs(x number) return number\n);\n/\n"
        val body = "create type body hidden_attribute as\n  member function fp(x number) return number is begin PARAMETER return x; end;\n" +
            "  member function fl return number is\n    x number := 30;\n  begin\n    LOCAL return x;\n  end;\n" +
            "  member function fs(x number) return number is begin SELF return self.x; end;\nend;\n/\n" + other
        fun mark(where: String) = spec + listOf("PARAMETER", "LOCAL", "SELF").fold(body) { text, marker -> text.replace(marker, if (marker == where) "/*A*/" else "") }
        assertSteps(mark("PARAMETER"), "x", listOf("Bound(PARAMETER x@9)"))
        assertSteps(mark("LOCAL"), "x", listOf("Bound(VARIABLE x@11)"), occurrence = 1)
        assertSteps(mark("SELF"), "self", listOf("ImplicitSelf(FUNCTION fs@15)", "Bound(ATTRIBUTE x@2)"))
        // Plain SELF is a value.
        assertThat(steps("create type self_value as object (x number, member function f return self_value);\n/\n" +
            "create type body self_value as\n  member function f return self_value is begin return self; end;\nend;\n/\n", "self", context = VALUE))
            .isEqualTo("ImplicitSelf(FUNCTION f@4)")
    }

    @Test
    fun anAttributeHidesASchemaFunction() {
        assertSteps(
            "create function zz_f return number is begin return 99; end;\n/\ncreate type attribute_like_function as object (zz_f number, member function f return number);\n/\n" +
                "create type body attribute_like_function as\n  member function f return number is begin /*A*/ return zz_f; end;\nend;\n/\n" + other,
            "zz_f", listOf("Bound(ATTRIBUTE zz_f@3)")
        )
    }

    @Test
    fun aNestedSubprogramOfAMemberMethodSeesAttributesAndSelf() {
        val source = "create type nested_in_member as object (x number, member function f return varchar2);\n/\n" +
            "create type body nested_in_member as\n  member function f return varchar2 is\n    r varchar2(20);\n" +
            "    procedure q is begin r := x || ' ' || self.x; end;\n  begin\n    /*A*/ q;\n    return r;\n  end;\nend;\n/\n" + other
        assertSteps(source, "x", listOf("Bound(ATTRIBUTE x@1)"))
        assertSteps(source, "self", listOf("ImplicitSelf(FUNCTION f@4)", "Bound(ATTRIBUTE x@1)"))
    }

    @Test
    fun aLocalNamedSelfCollidesWithTheImplicitSelf() {
        // PLS-00410.
        assertSteps(
            "create type local_self as object (x number, member function f return number);\n/\n" +
                "create type body local_self as\n  member function f return number is\n    self number := 5;\n  begin\n    /*A*/ return self;\n  end;\nend;\n/\n" + other,
            "self", listOf("Duplicate(VARIABLE self@5)"), context = VALUE
        )
    }

    @Test
    fun aStaticMethodHasNoSelfAndRejectsInstanceMembers() {
        val spec = "create type static_method as object (\n  x number,\n  member function m return number,\n" +
            "  static function s1 return number,\n  static function s2 return number\n);\n/\n"
        fun body(statement: String) = spec + "create type body static_method as\n  member function m return number is begin return 1; end;\n" +
            "  static function s1 return number is begin return 8; end;\n  static function s2 return number is begin /*A*/ $statement end;\nend;\n/\n" + other
        // `self.x` is PLS-00201, an error rather than an absence.
        assertSteps(body("return self.x;"), "self", listOf("Undeclared"))
        // An attribute (PLS-00588) and a member method (PLS-00306) capture the name and are rejected.
        assertSteps(body("return x;"), "x", listOf("InstanceMemberInStatic(ATTRIBUTE x@2)"))
        assertSteps(body("return m();"), "m", listOf("InstanceMemberInStatic(FUNCTION m@9)"), context = CALL_EXPRESSION)
        // Another static method is callable.
        assertSteps(body("return s1();"), "s1", listOf("OverloadSet(FUNCTION s1@10)"), context = CALL_EXPRESSION)
    }

    @Test
    fun constructorsSeeSelfImplicitlyOrExplicitly() {
        val source = "create type constructed as object (\n  x number,\n  constructor function constructed(v number) return self as result,\n" +
            "  constructor function constructed(v number, w number) return self as result\n);\n/\n" +
            "create type body constructed as\n  constructor function constructed(v number) return self as result is\n  begin\n    /*A*/ self.x := v;\n    return;\n  end;\n" +
            "  constructor function constructed(self in out constructed, v number, w number) return self as result is\n  begin\n" +
            "    x := v + w;\n    self.x := x;\n    return;\n  end;\nend;\n/\n" + other
        assertSteps(source, "self", listOf("ImplicitSelf(FUNCTION constructed@8)", "Bound(ATTRIBUTE x@2)"))
        assertThat(steps(source.replace("/*A*/", "").replace("/*S*/", ""), "x", 1)).isEqualTo("Bound(ATTRIBUTE x@2)")
        // The explicit `SELF IN OUT t` parameter is the same SELF.
        assertThat(steps(source.replace("/*A*/", "").replace("/*S*/", ""), "self", 1))
            .isEqualTo("Bound(PARAMETER self@13) / Bound(ATTRIBUTE x@2)")
    }

    @Test
    fun aSubtypeSeesSupertypeAttributesButNotItsMethodsUnqualified() {
        val base = "create type base_type as object (a number, member function base_m return number) not final;\n/\n" +
            "create type body base_type as\n  member function base_m return number is begin return a + 1; end;\nend;\n/\n"
        val sub = "create type sub_type under base_type (b number, member function g return number);\n/\n" +
            "create type body sub_type as\n  member function g return number is begin /*A*/ return a + b + base_m() + self.base_m(); end;\nend;\n/\n" + other
        assertSteps(base + sub, "a", listOf("Bound(ATTRIBUTE a@1)"), occurrence = 1)
        assertSteps(base + sub, "b", listOf("Bound(ATTRIBUTE b@7)"))
        // A supertype method is not visible unqualified (PLS-00201), only through SELF.
        assertSteps(base + sub, "base_m", listOf("ProvenAbsent"), context = CALL_EXPRESSION)
        assertSteps(base + sub, "self", listOf("ImplicitSelf(FUNCTION g@10)", "OverloadSet(FUNCTION base_m@1)"))
        // Without the supertype in the file it may declare any name.
        assertThat(steps(sub.replace("/*A*/", "").replace("/*S*/", ""), "a"))
            .isEqualTo("Incomplete[SUPERTYPE_NOT_IN_FILE](ProvenAbsent)")
        // A subtype cannot redeclare a supertype attribute (PLS-00410): its own attribute decides without the supertype.
        assertThat(steps(sub.replace("/*A*/", "").replace("/*S*/", ""), "b")).isEqualTo("Bound(ATTRIBUTE b@1)")
    }

    @Test
    fun aTypeBodyWithoutItsSpecificationReliesOnIt() {
        val body = "create type body lone_body as\n  member function f return number is begin /*A*/ return a; end;\nend;\n/\n" + other
        assertThat(steps(body.replace("/*A*/", "").replace("/*S*/", ""), "a")).isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](ProvenAbsent)")
        assertThat(steps(body.replace("/*A*/", "\$IF \$\$x \$THEN NULL; \$END").replace("/*S*/", ""), "a"))
            .isEqualTo("Incomplete[CONDITIONAL_COMPILATION, SPECIFICATION_NOT_IN_FILE](ProvenAbsent)")
        assertThat(steps(body.replace("/*A*/", "").replace("/*S*/", "\$IF \$\$x \$THEN NULL; \$END"), "a"))
            .isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](ProvenAbsent)")
        // An incomplete type followed by its definition is one type.
        assertThat(steps("create type completed;\n/\ncreate type completed as object (n number, member function twice return number);\n/\n" +
            "create type body completed as\n  member function twice return number is begin return n * 2; end;\nend;\n/\n", "n"))
            .isEqualTo("Bound(ATTRIBUTE n@3)")
    }

    @Test
    fun attributesAndMethodsCaptureTypeNames() {
        // An attribute type named like another attribute (PLS-00588) or a method (PLS-00488).
        assertThat(steps("create type attribute_as_type as object (os_t number, b os_t);\n/\n", "os_t", context = TYPE))
            .isEqualTo("WrongKind(ATTRIBUTE os_t@1)")
        assertThat(steps("create type method_as_type as object (b os_t, member function os_t return number);\n/\n", "os_t", context = TYPE))
            .isEqualTo("WrongKind(FUNCTION os_t@1)")
        // A method parameter type, in the specification and in the body (PLS-00488).
        assertThat(steps("create type parameter_type as object (os_t number, member function m(p os_t) return number);\n/\n", "os_t", context = TYPE))
            .isEqualTo("WrongKind(ATTRIBUTE os_t@1)")
        // A local's type in a member method body.
        assertSteps(
            "create type local_type as object (os_t number, member function m return number);\n/\n" +
                "create type body local_type as\n  member function m return number is\n    v os_t;\n  begin\n    /*A*/ return 1;\n  end;\nend;\n/\n" + other,
            "os_t", listOf("WrongKind(ATTRIBUTE os_t@1)"), context = TYPE
        )
        // A name the type does not declare is left to the schema.
        assertThat(steps("create type plain_type as object (b os_t);\n/\n", "os_t", context = TYPE)).isEqualTo("ProvenAbsent")
    }

    @Test
    fun aStaticMethodCannotBeInvokedThroughSelf() {
        // PLS-00587.
        assertSteps(
            "create type static_through_self as object (x number, static function s return number, member function m return number);\n/\n" +
                "create type body static_through_self as\n  static function s return number is begin return 1; end;\n" +
                "  member function m return number is begin /*A*/ return self.s(); end;\nend;\n/\n" + other,
            "self", listOf("ImplicitSelf(FUNCTION m@5)", "WrongKind(FUNCTION s@4)")
        )
    }

    @Test
    fun aSelfMethodStepReliesOnAnAbsentSupertype() {
        // A subtype may overload an inherited method name: an absent supertype may add overloads.
        val source = "create type sb under absent_base (b number, overriding member function m return number, member function g return number);\n/\n" +
            "create type body sb as\n  overriding member function m return number is begin return 1; end;\n" +
            "  member function g return number is begin return self.m() + self.b; end;\nend;\n/\n"
        assertThat(steps(source, "self")).isEqualTo("ImplicitSelf(FUNCTION g@5) / Incomplete[SUPERTYPE_NOT_IN_FILE](OverloadSet(FUNCTION m@4))")
        // Its own attribute does not: attributes cannot repeat an inherited name (PLS-00410).
        assertThat(steps(source, "self", 1)).isEqualTo("ImplicitSelf(FUNCTION g@5) / Bound(ATTRIBUTE b@1)")
        // Control: with the supertype in the file its overloads are seen and nothing is missing.
        val base = "create type absent_base as object (a number, member function m return number) not final;\n/\n"
        support.assertSteps(
            base + source.replace("return 1; end;", "begin /*A*/ return 1; end;".removePrefix("begin ")) + "create procedure other is begin /*S*/ null; end;\n/\n",
            "self", listOf("ImplicitSelf(FUNCTION g@7)", "OverloadSet(FUNCTION m@1, FUNCTION m@6)"), fromStep = 1
        )
    }

    @Test
    fun aLoneTypeBodyHasNoOrder() {
        // A method called before its definition: the body implements only what the specification declares.
        assertThat(steps("create type body lone_order as\n  member function f return number is begin return g(); end;\n" +
            "  member function g return number is begin return 1; end;\nend;\n/\n", "g", context = CALL_EXPRESSION))
            .isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](OverloadSet(FUNCTION g@3))")
    }

    @Test
    fun membersOfAnAttributeNeedTyping() {
        val source = "create type typed_attribute as object (b other_t, member function f return number);\n/\n" +
            "create type body typed_attribute as\n  member function f return number is begin return b.n + self.b.n; end;\nend;\n/\n"
        assertThat(steps(source, "b", context = QUALIFIER)).isEqualTo("Bound(ATTRIBUTE b@1) / Unsupported(MEMBER_NEEDS_TYPING)")
        assertThat(steps(source, "self")).isEqualTo("ImplicitSelf(FUNCTION f@4) / Bound(ATTRIBUTE b@1) / Unsupported(MEMBER_NEEDS_TYPING)")
    }

    @Test
    fun objectTypeOutcomesAreDeterministic() {
        val source = "create type base_d as object (a number) not final;\n/\ncreate type sub_d under base_d (b number, member function g return number," +
            " static function s return number);\n/\ncreate type body sub_d as\n  member function g return number is begin return a + self.b; end;\n" +
            "  static function s return number is begin return b; end;\nend;\n/\n"
        val analysis = LocalResolverTest().analyze(source)
        fun all(order: (List<LocalReference>) -> List<LocalReference>): List<String> {
            val outcomes = order(analysis.references).associateWith { LocalResolver.resolveQualified(analysis.model, it) }
            return analysis.references.map { "$it -> ${outcomes.getValue(it).joinToString(" / ") { o -> LocalResolverTest().render(analysis.model, o) }}" }
        }
        val forward = all { it }
        assertThat(all { it.reversed() }).isEqualTo(forward)
        assertThat(forward).anyMatch { it.endsWith("ImplicitSelf(FUNCTION g@6) / Bound(ATTRIBUTE b@3)") }
            .anyMatch { it.endsWith("InstanceMemberInStatic(ATTRIBUTE b@3)") }.anyMatch { it.endsWith("Bound(ATTRIBUTE a@1)") }
    }
}
