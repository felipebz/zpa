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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectCallArguments
import com.felipebz.zpa.api.project.ProjectCallKind
import com.felipebz.zpa.api.project.ProjectCallResolution
import com.felipebz.zpa.api.project.ProjectCallTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LostDeclarationsTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun names(text: String) = extractor.extract(FileId("f.sql"), text).map { "${it.kind} ${it.qualifiedName()}" }

    private val bodyWithoutSemicolon = "CREATE OR REPLACE PACKAGE BODY p1 AS\n  PROCEDURE a IS BEGIN NULL; END;\nEND p1\n/\n"

    @Test
    fun theUnitAfterAPackageBodyWithoutSemicolonIsKept() {
        assertThat(names(bodyWithoutSemicolon + "CREATE OR REPLACE PROCEDURE q IS BEGIN NULL; END;\n/\n"))
            .containsExactly("PACKAGE_PROCEDURE p1.a", "STANDALONE_PROCEDURE q")
        assertThat(names(bodyWithoutSemicolon + "CREATE OR REPLACE PACKAGE p2 AS PROCEDURE b; END p2;\n/\n"))
            .containsExactly("PACKAGE_PROCEDURE p1.a", "PACKAGE p2", "PACKAGE_PROCEDURE p2.b")
    }

    @Test
    fun aBrokenPackageBodyNeverTakesTheNextUnit() {
        val q = "CREATE OR REPLACE PROCEDURE q IS BEGIN NULL; END;\n/\n"
        val head = "CREATE OR REPLACE PACKAGE BODY p1 AS\n  PROCEDURE a IS BEGIN NULL; END;\n"
        // Without its final END the body would otherwise read q as its own subprogram p1.q.
        assertThat(names(head + "/\n" + q)).containsExactly("PACKAGE_PROCEDURE p1.a", "STANDALONE_PROCEDURE q")
        assertThat(names(head + "BEGIN NULL; END p1\n/\n" + q)).containsExactly("PACKAGE_PROCEDURE p1.a", "STANDALONE_PROCEDURE q")
        assertThat(names(head + "END p1\n" + q)).containsExactly("PACKAGE_PROCEDURE p1.a", "STANDALONE_PROCEDURE q")
    }

    @Test
    fun aCallFromAnotherFileReachesTheUnitAfterTheBody() {
        val snapshot = ProjectSnapshots.prepare(listOf(
            ProjectSourceInput.ofText("body.sql", bodyWithoutSemicolon + "CREATE OR REPLACE PROCEDURE q IS BEGIN NULL; END;\n/\n"),
            ProjectSourceInput.ofText("caller.sql", "CREATE PROCEDURE caller IS BEGIN q; END;\n/\n")
        ), false)
        val resolution = snapshot.resolveCall(
            ProjectCallTarget.written(ProjectQualifiedName.of("q")), ProjectCallKind.PROCEDURE, ProjectCallArguments.none()
        )
        assertThat(resolution.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(resolution.selected.get().declaration.fileId).isEqualTo("body.sql")
    }

    @Test
    fun polymorphicTableFunctionsAreFunctionsWithPseudoTypedParameters() {
        val declarations = extractor.extract(FileId("f.sql"),
            "CREATE FUNCTION skip_col(tab TABLE, col COLUMNS) RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;\n/\n" +
                "CREATE PACKAGE pk AS\n  FUNCTION describe(tab IN OUT DBMS_TF.TABLE_T) RETURN DBMS_TF.DESCRIBE_T;\n" +
                "  FUNCTION ptf(tab TABLE) RETURN TABLE PIPELINED TABLE POLYMORPHIC USING pk;\nEND;\n/\n")
        val skipCol = declarations.filterIsInstance<StandaloneFunctionDeclaration>().single()
        assertThat(skipCol.parameters.map { "${it.name.originalSpelling} ${it.typeRef.name}" }).containsExactly("tab TABLE", "col COLUMNS")
        assertThat(skipCol.returnType.name.toString()).isEqualTo("TABLE")
        val ptf = declarations.filterIsInstance<PackageFunctionDeclaration>().single { it.name.lookupName == "PTF" }
        assertThat(ptf.parameters.single().typeRef.name.toString()).isEqualTo("TABLE")
    }

    @Test
    fun thePseudoTypesNeverResolveToASchemaType() {
        // Only a quoted "TABLE" can be a schema type (both words are reserved in PL/SQL).
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(listOf(
            ProjectSource(FileId("t.sql")) { "CREATE TYPE \"TABLE\" AS OBJECT (a NUMBER);\n/\n" }
        ), concurrent = false))
        val resolver = ProjectTypeResolver(context)
        val range = SourceRange(FileId("u.sql"), 1, 0, 1, 5)
        fun resolve(name: String) = resolver.resolve(NamedTypeRef(QualifiedName(listOf(OracleIdentifier.fromSource(name))), range))
        assertThat(resolve("TABLE")).isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThat(resolve("columns")).isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThat(resolve("\"TABLE\"")).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
    }

    @Test
    fun aMethodWithAccessibleByIsKept() {
        val type = extractor.extract(FileId("f.sql"),
            "CREATE TYPE t AS OBJECT (\n  a NUMBER,\n  MEMBER FUNCTION f RETURN NUMBER ACCESSIBLE BY (PACKAGE p),\n  MEMBER PROCEDURE g\n);\n/\n")
            .filterIsInstance<StandaloneTypeDeclaration>().single()
        assertThat(type.methods.map { it.name.originalSpelling }).containsExactly("f", "g")
        assertThat(type.methods.first().returnType!!.name.toString()).isEqualTo("NUMBER")
        assertThat(type.methodsComplete).isTrue()
        // Still fine: SHARING on a type, ACCESSIBLE BY on standalone units and packages.
        assertThat(names("CREATE TYPE s SHARING = METADATA AS OBJECT (a NUMBER);\n/\n" +
            "CREATE PROCEDURE sp ACCESSIBLE BY (PACKAGE p) IS BEGIN NULL; END;\n/\n" +
            "CREATE PACKAGE pk ACCESSIBLE BY (PACKAGE p) AS PROCEDURE m; END;\n/\n"))
            .containsExactly("STANDALONE_TYPE s", "STANDALONE_PROCEDURE sp", "PACKAGE pk", "PACKAGE_PROCEDURE pk.m")
    }
}
