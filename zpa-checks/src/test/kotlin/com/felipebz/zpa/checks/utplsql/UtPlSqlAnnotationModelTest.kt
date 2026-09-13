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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class UtPlSqlAnnotationModelTest {

    @Test
    fun associatesAnnotationGroupsWithProceduresAndFunctions() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              -- %test(one)
              -- %tags(fast)
              PROCEDURE first;

              -- %beforeall
              FUNCTION second(
                  value NUMBER
              ) RETURN NUMBER;

              -- %suite
              END p;
            """
        )

        val procedureGroup = model.groups.single { it.declarationNode?.declaredName() == "first" }
        assertThat(procedureGroup.annotations.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.TEST, UtPlSqlAnnotationKind.TAGS)
        assertThat(procedureGroup.packageNode.type).isEqualTo(PlSqlGrammar.CREATE_PACKAGE)

        val functionGroup = model.groups.single { it.declarationNode?.declaredName() == "second" }
        assertThat(functionGroup.annotations.single().kind).isEqualTo(UtPlSqlAnnotationKind.BEFOREALL)

        val packageGroup = model.groups.single { it.declarationNode == null }
        assertThat(packageGroup.annotations.single().kind).isEqualTo(UtPlSqlAnnotationKind.SUITE)
    }

    @Test
    fun blankLinesBreakAssociation() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%test



              PROCEDURE first;
            END p;
            """
        )

        assertThat(model.groups.single { it.annotations.single().kind == UtPlSqlAnnotationKind.TEST }.declarationNode)
            .isNull()
    }

    @Test
    fun floatingAnnotationsRemainPackageLevelWhileTheFollowingTestAttaches() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%ann1(one)
              --%ann2(two)

              --%test
              PROCEDURE first;
            END p;
            """
        )

        val floating = model.groups
            .filter { it.declarationNode == null }
            .flatMap { it.annotations }
        assertThat(floating.map { it.name }).containsExactly("ann1", "ann2")
        assertThat(model.groups.single { it.annotations.any { annotation -> annotation.kind == UtPlSqlAnnotationKind.TEST } }
            .declarationNode?.declaredName()).isEqualTo("first")
    }

    @Test
    fun aBlankLineSeparatesPackageHooksFromProcedureAnnotations() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%beforeall(setup)

              --%test
              PROCEDURE first;
            END p;
            """
        )

        assertThat(model.groups.single { it.annotations.single().kind == UtPlSqlAnnotationKind.BEFOREALL }
            .declarationNode).isNull()
        assertThat(model.groups.single { it.annotations.single().kind == UtPlSqlAnnotationKind.TEST }
            .declarationNode?.declaredName()).isEqualTo("first")
    }

    @Test
    fun ordinaryCommentsBreakAssociationButTheFollowingAnnotationCanStillAttach() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%test
              -- explanation for humans
              PROCEDURE first;

              --%test
              -- another explanation
              --%tags(slow)
              PROCEDURE second;
            END p;
            """
        )

        val packageGroups = model.groups.filter { it.declarationNode == null }
        assertThat(packageGroups.flatMap { it.annotations }.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.TEST, UtPlSqlAnnotationKind.TEST)
        assertThat(model.groups.single { it.declarationNode?.declaredName() == "second" }.annotations.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.TAGS)
    }

    @Test
    fun unknownAnnotationsRemainInTheirAnnotationGroup() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              -- %future_annotation(value)
              -- %TeSt
              PROCEDURE first;
            END p;
            """
        )

        val group = model.groups.single { it.declarationNode != null }
        assertThat(group.annotations.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.UNKNOWN, UtPlSqlAnnotationKind.TEST)
        assertThat(group.annotations[0].argument).isEqualTo("value")
    }

    @Test
    fun packageBodyDeclarationsAreModeledSeparatelyFromPackageSpecificationDeclarations() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%test
              PROCEDURE declared;
            END p;

            CREATE PACKAGE BODY p AS
              --%test
              PROCEDURE implemented IS
              BEGIN
                NULL;
              END implemented;
            END p;
            """
        )

        val groups = model.groups.filter { it.declarationNode != null }
        assertThat(groups).hasSize(2)
        assertThat(groups.map { it.packageNode.type })
            .containsExactly(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
    }

    @Test
    fun annotationsAfterADeclarationRemainPackageLevel() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              PROCEDURE first;
              --%suitepath(example)
            END p;
            """
        )

        assertThat(model.groups.single { it.declarationNode == null }.annotations.single().kind)
            .isEqualTo(UtPlSqlAnnotationKind.SUITEPATH)
    }

    @Test
    fun allAnnotationsInAProcedureRunBelongToTheProcedure() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%suite
              --%test
              PROCEDURE first;
            END p;
            """
        )

        val procedureGroup = model.groups.single { it.declarationNode?.declaredName() == "first" }
        assertThat(procedureGroup.annotations.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.SUITE, UtPlSqlAnnotationKind.TEST)
        assertThat(model.groups.filter { it.declarationNode == null }).isEmpty()
    }

    @Test
    fun annotationArgumentsDoNotChangeLexicalAssociation() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%beforeall(setup)
              --%test
              PROCEDURE first;
            END p;
            """
        )

        assertThat(model.groups.single { it.declarationNode?.declaredName() == "first" }
            .annotations.map { it.kind })
            .containsExactly(UtPlSqlAnnotationKind.BEFOREALL, UtPlSqlAnnotationKind.TEST)
    }

    @Test
    fun annotationsBeforeThePackageAreNotAttributedToIt() {
        val model = collect(
            """
            --%suite
            CREATE PACKAGE p AS
            END p;
            """
        )

        assertThat(model.groups).isEmpty()
    }

    @Test
    fun unrelatedSourceBetweenAnnotationAndDeclarationBreaksAssociation() {
        val model = collect(
            """
            CREATE PACKAGE p AS
              --%test
              marker NUMBER;
              PROCEDURE first;
            END p;
            """
        )

        assertThat(model.groups.single().declarationNode).isNull()
        assertThat(model.groups.single().annotations.single().kind)
            .isEqualTo(UtPlSqlAnnotationKind.TEST)
    }

    @Test
    fun inlineAnnotationLikeCommentsAreIgnored() {
        val model = collect(
            """
            CREATE PACKAGE BODY p AS
              PROCEDURE first IS
              BEGIN
                NULL; --%test
              END first;
            END p;
            """
        )

        assertThat(model.groups).isEmpty()
    }

    private fun collect(source: String): UtPlSqlAnnotationModel =
        UtPlSqlAnnotationCollector.collect(
            PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source.trimIndent())
        )
}

private fun AstNode.declaredName(): String = tokens[1].originalValue
