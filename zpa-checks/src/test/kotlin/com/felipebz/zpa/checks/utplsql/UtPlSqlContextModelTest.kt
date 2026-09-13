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
package com.felipebz.zpa.checks.utplsql

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class UtPlSqlContextModelTest {

    @Test
    fun buildsNestedContextsFromEffectivePackageAnnotations() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Outer description)
              --%name(outer_name)

              --%test
              PROCEDURE outer_test;

              --%context(Inner description)
              --%name(inner_name)

              --%test
              PROCEDURE inner_test;
              --%endcontext

              --%endcontext
            END p;
            """
        )

        val packageModel = model.packages.single()
        assertThat(packageModel.packageNode.type).isEqualTo(PlSqlGrammar.CREATE_PACKAGE)
        assertThat(packageModel.contexts).hasSize(2)
        assertThat(packageModel.contexts[0].parentIndex).isNull()
        assertThat(packageModel.contexts[0].childIndices).containsExactly(1)
        assertThat(packageModel.contexts[1].parentIndex).isEqualTo(0)
        assertThat(packageModel.contexts[1].childIndices).isEmpty()
        assertThat(packageModel.contexts[0].selectedNameAnnotation?.argument).isEqualTo("outer_name")
        assertThat(packageModel.contexts[1].selectedNameAnnotation?.argument).isEqualTo("inner_name")
        assertThat(packageModel.contexts.all { it.closingAnnotation != null }).isTrue()
    }

    @Test
    fun keepsPackageAnnotationsInsideTheCurrentContextAndHandlesMalformedBoundaries() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%name(outside)

              --%context(Outer)
              --%name(first_name)
              --%tags(tag)
              --%name(second_name)
              --%context(Inner)
              --%name(inner_name)
              --%endcontext
              --%endcontext
              --%endcontext
            END p;
            """
        )

        val packageModel = model.packages.single()
        assertThat(packageModel.nameAnnotationsOutsideContexts.map { it.argument })
            .containsExactly("outside")
        assertThat(packageModel.unmatchedEndContextAnnotations).hasSize(1)
        assertThat(packageModel.contexts[0].selectedNameAnnotation?.argument).isEqualTo("first_name")
        assertThat(packageModel.contexts[0].duplicateNameAnnotations.map { it.argument })
            .containsExactly("second_name")
        assertThat(packageModel.contexts[1].parentIndex).isEqualTo(0)
    }

    @Test
    fun selectsTheFirstNameRegardlessOfItsSyntaxOrValue() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Invalid first)
              --%name(invalid.name)
              --%name(valid_name)
              --%endcontext

              --%context(Empty first)
              --%name()
              --%name(valid_name)
              --%endcontext

              --%context(Malformed first)
              --%name(unclosed
              --%name(valid_name)
              --%endcontext
            END p;
            """
        )

        assertThat(model.packages.single().contexts).hasSize(3)
        assertThat(model.packages.single().contexts.map { it.selectedNameAnnotation?.argumentSyntax })
            .containsExactly(
                UtPlSqlAnnotationArgumentSyntax.PARENTHESIZED,
                UtPlSqlAnnotationArgumentSyntax.PARENTHESIZED,
                UtPlSqlAnnotationArgumentSyntax.MALFORMED
            )
        assertThat(model.packages.single().contexts.map { it.selectedNameAnnotation?.argument })
            .containsExactly("invalid.name", null, null)
        assertThat(model.packages.single().contexts.map { it.duplicateNameAnnotations.map(UtPlSqlAnnotation::argument) })
            .containsExactly(
                listOf("valid_name"),
                listOf("valid_name"),
                listOf("valid_name")
            )
    }

    @Test
    fun closesAContextNameWindowWhenTheFirstNestedContextStarts() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Outer)
              --%name(valid_outer)
              --%context(Inner)
              --%name(inner_name)
              --%endcontext
              --%name(too_late)
              --%endcontext
            END p;
            """
        )

        val contexts = model.packages.single().contexts
        assertThat(contexts[0].selectedNameAnnotation?.argument).isEqualTo("valid_outer")
        assertThat(contexts[0].duplicateNameAnnotations).isEmpty()
        assertThat(contexts[0].lateNameAnnotations.map { it.argument }).containsExactly("too_late")
    }

    @Test
    fun matchesUpstreamRecoveryWhenNoEndContextExists() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Outer)
              --%context(Inner)
              -- package-level boundary before the test annotation
              --%test
              PROCEDURE test_without_endcontext;
            END p;
            """
        )

        val contexts = model.packages.single().contexts
        assertThat(contexts).hasSize(1)
        assertThat(contexts.single().openingAnnotation.argument).isEqualTo("Outer")
        assertThat(contexts.single().closingAnnotation).isNull()
        assertThat(model.packages.single().duplicateContextAnnotations.map { it.argument })
            .containsExactly("Inner")
    }

    @Test
    fun usesTheCurrentContextPositionForRecoveryAfterAnEarlierEndContext() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Already closed)
              --%endcontext
              --%context(Outer)
              --%context(Inner)
              -- package-level boundary before the test annotation
              --%test
              PROCEDURE test_without_outer_endcontext;
            END p;
            """
        )

        val packageModel = model.packages.single()
        assertThat(packageModel.contexts.map { it.openingAnnotation.argument })
            .containsExactly("Already closed", "Outer")
        assertThat(packageModel.contexts[1].closingAnnotation).isNull()
        assertThat(packageModel.duplicateContextAnnotations.map { it.argument })
            .containsExactly("Inner")
    }

    @Test
    fun excludesProcedureAssociatedAndPackageBodyContextAnnotations() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%context(Procedure context)
              --%test
              PROCEDURE direct_test;
            END p;

            CREATE PACKAGE BODY p AS
              --%context(Body context)
              --%endcontext
            END p;
            """
        )

        assertThat(model.packages).hasSize(1)
        assertThat(model.packages.single().contexts).isEmpty()
    }

    @Test
    fun keepsSeparatePackagesIndependent() {
        val model = collect(
            """
            CREATE PACKAGE first AS
              --%suite
              --%context(First)
              --%endcontext
            END first;

            CREATE PACKAGE second AS
              --%suite
              --%context(Second)
              --%endcontext
            END second;
            """
        )

        assertThat(model.packages).hasSize(2)
        assertThat(model.packages.map { it.contexts.single().openingAnnotation.argument })
            .containsExactly("First", "Second")
    }

    @Test
    fun assignsAnnotationsToTheirEffectiveRootOrContextScope() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%displayname(Root name)

              --%context(Outer)
              --%displayname(Outer name)
              --%context(Inner)
              --%displayname(Inner name)
              --%endcontext
              --%rollback(auto)
              --%endcontext

              --%rollback(manual)
            END p;
            """
        )

        val packageModel = model.packages.single()
        assertThat(packageModel.rootAnnotations.map { it.kind to it.argument })
            .containsExactly(
                UtPlSqlAnnotationKind.SUITE to null,
                UtPlSqlAnnotationKind.DISPLAYNAME to "Root name",
                UtPlSqlAnnotationKind.ROLLBACK to "manual"
            )

        assertThat(packageModel.contexts[0].annotations.map { it.kind to it.argument })
            .containsExactly(
                UtPlSqlAnnotationKind.CONTEXT to "Outer",
                UtPlSqlAnnotationKind.DISPLAYNAME to "Outer name",
                UtPlSqlAnnotationKind.ROLLBACK to "auto",
                UtPlSqlAnnotationKind.ENDCONTEXT to null
            )
        assertThat(packageModel.contexts[1].annotations.map { it.kind to it.argument })
            .containsExactly(
                UtPlSqlAnnotationKind.CONTEXT to "Inner",
                UtPlSqlAnnotationKind.DISPLAYNAME to "Inner name",
                UtPlSqlAnnotationKind.ENDCONTEXT to null
            )
    }

    private fun collect(source: String): UtPlSqlContextModel =
        UtPlSqlContextCollector.collect(
            PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source.trimIndent())
        )
}
