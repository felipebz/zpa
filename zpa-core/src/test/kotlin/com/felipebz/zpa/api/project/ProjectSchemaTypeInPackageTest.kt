//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.local.LocalTypeName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProjectSchemaTypeInPackageTest {
    private val types = "CREATE TYPE str_list AS TABLE OF VARCHAR2(4000);\n/\nCREATE TYPE point AS OBJECT (x NUMBER, y NUMBER);\n/\n"
    private val pk = "CREATE PACKAGE pk AS\n  FUNCTION f(p str_list) RETURN point;\nEND pk;\n/\n" +
        "CREATE PACKAGE BODY pk AS\n  FUNCTION f(p str_list) RETURN point IS v point; BEGIN v := point(1, 2); RETURN v; END;\nEND pk;\n/\n"

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun prepare(vararg files: Pair<String, String>) =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)
    private fun resolve(s: ProjectSnapshot, name: String) = s.resolveNamedType(n(name), n("pk"))

    @Test
    fun aPackageOwnerReachesSchemaTypesOnlyWhereThePackageProvesTheNameAbsent() {
        val s = prepare("types.sql" to types, "pk.sql" to pk)
        resolve(s, "point").let {
            assertThat(it.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
            assertThat(it.declaration.get().kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        }
        val own = prepare("types.sql" to types, "pk.sql" to "CREATE PACKAGE pk AS TYPE point IS RECORD (z NUMBER); END pk;\n/\n")
        assertThat(resolve(own, "point").declaration.get().kind).isEqualTo(ProjectDeclarationKind.PACKAGE_TYPE)
        val conditional = prepare("types.sql" to types, "pk.sql" to "CREATE PACKAGE pk AS \$IF \$\$f \$THEN TYPE other IS RECORD (z NUMBER); \$END END pk;\n/\n")
        // Conditional text that does not write the name proves it absent; text that writes it does not.
        assertThat(resolve(conditional, "point").status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        val mentioned = prepare("types.sql" to types, "pk.sql" to "CREATE PACKAGE pk AS \$IF \$\$f \$THEN TYPE other IS RECORD (z NUMBER); \$ELSE TYPE point IS RECORD (z NUMBER); \$END END pk;\n/\n")
        assertThat(resolve(mentioned, "point").status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
        val noSpec = prepare("types.sql" to types)
        assertThat(resolve(noSpec, "point").status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
        val twice = prepare("types.sql" to types, "again.sql" to "CREATE TYPE point AS OBJECT (x NUMBER);\n/\n", "pk.sql" to pk)
        assertThat(resolve(twice, "point").status).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS)
    }

    @Test
    fun packageValuesTypedBySchemaTypesAreDescribed() {
        val s = prepare("types.sql" to types, "pk.sql" to "CREATE PACKAGE pk AS v point; l str_list; END pk;\n/\n")
        for (name in listOf("v", "l")) {
            val value = s.findDeclarations(n("pk", name)).declarations.single() as ProjectPackageValueView
            val description = s.describeType(value.typeRef, value.owner)
            assertThat(description.status).describedAs(name).isEqualTo(ProjectTypeDescription.Status.DECLARED_TYPE)
            assertThat(description.declaration.get().kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        }
    }

    @Test
    fun theSnapshotAndTheAnalysisAgree() {
        val s = prepare("types.sql" to types, "pk.sql" to pk)
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            listOf("types.sql" to types, "pk.sql" to pk).map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val analysis = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (datatype in node.getDescendants(PlSqlGrammar.DATATYPE)) {
                    val name = this.context.valueCategories?.typeName(datatype) as? LocalTypeName.Project ?: continue
                    analysis += "${datatype.tokenOriginalValue}@${name.declaration.fileId}"
                }
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        scanner.scanFile(Source("pk.sql", pk), listOf(check), FileId("pk.sql"))
        val snapshot = listOf("str_list", "point", "str_list", "point", "point").map { name ->
            "$name@${resolve(s, name).declaration.get().fileId}"
        }
        assertThat(analysis).isEqualTo(snapshot)
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
