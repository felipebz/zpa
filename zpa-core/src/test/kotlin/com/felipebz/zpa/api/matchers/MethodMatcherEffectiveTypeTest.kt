/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.matchers

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.datatype.CharacterDatatype
import com.felipebz.zpa.project.AnchoredTypeRef
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.PackageTypeDeclaration
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectRecordField
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathSegment
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.ProjectTypeDeclaration
import com.felipebz.zpa.project.ProjectTypeResolution
import com.felipebz.zpa.project.ProjectTypeShape
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.TypeAnchor
import com.felipebz.zpa.project.TypeRef
import com.felipebz.zpa.project.TypeRefSemanticResolution
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.EffectiveSemanticTypeQuery
import com.felipebz.zpa.symbols.EffectiveTypeCategory
import com.felipebz.zpa.symbols.EffectiveTypeSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class MethodMatcherEffectiveTypeTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun usesAProjectBuiltInFieldCategory() {
        val call = callWithFieldType("value.id", "NUMBER", PlSqlType.NUMERIC)

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isTrue
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.CHARACTER).matches(call)).isFalse
    }

    @Test
    fun usesProjectRecordCategoryFromTheSemanticPipeline() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN func(value); END;"
        )
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        ))
        val check = MatcherCheck(MethodMatcher.create().name("func").addParameter(PlSqlType.RECORD))
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)

        scanner.scanFile(FixtureFile(packageFile, sources[0].second))
        scanner.scanFile(FixtureFile(useFile, sources[1].second), listOf(check), useFile)

        assertThat(check.projectTypeDeclaration).isNotNull
        assertThat(check.matched).isTrue
        assertThat(check.legacyArgumentType).isEqualTo(PlSqlType.UNKNOWN)
    }

    @Test
    fun usesProjectBuiltInFieldCategoryFromTheSemanticPipeline() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN func(value.id); END;"
        )
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        ))
        val check = ProjectFieldMatcherCheck(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC))
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)

        scanner.scanFile(FixtureFile(packageFile, sources[0].second))
        scanner.scanFile(FixtureFile(useFile, sources[1].second), listOf(check), useFile)

        assertThat(check.matched).isTrue
        assertThat(check.legacyArgumentType).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(check.projectField?.name).isEqualTo(identifier("id"))
        assertThat(check.effectiveCategory).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.NUMERIC, EffectiveTypeSource.BUILT_IN_TYPE_REF)
        )
    }

    @Test
    fun usesAProjectCharacterFieldCategory() {
        val call = callWithFieldType("value.name", "VARCHAR2", PlSqlType.CHARACTER)

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.CHARACTER).matches(call)).isTrue
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isFalse
    }

    @Test
    fun usesAProjectRecordFieldCategory() {
        val reference = named("p.child")
        val call = call("value.child") { argument ->
            argument.projectRecordFieldTypeResolution = fieldTypeResolution(
                field("child", reference),
                TypeRefSemanticResolution.Project(
                    reference,
                    ProjectTypeResolution.Resolved(reference, record("child"))
                )
            )
        }

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.RECORD).matches(call)).isTrue
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isFalse
    }

    @Test
    fun usesACompletedProjectPathTerminalCategory() {
        val childReference = named("p.child")
        val child = field("child", childReference)
        val idReference = named("NUMBER")
        val id = field("id", idReference)
        val call = call("value.child.id") { argument ->
            argument.projectRecordMemberPathResolution = ProjectRecordMemberPathResolution.Completed(listOf(
                ProjectRecordMemberPathSegment(child, fieldTypeResolution(
                    child,
                    TypeRefSemanticResolution.Project(
                        childReference,
                        ProjectTypeResolution.Resolved(childReference, record("child"))
                    )
                )),
                ProjectRecordMemberPathSegment(id, fieldTypeResolution(
                    id,
                    TypeRefSemanticResolution.BuiltIn(idReference, PlSqlType.NUMERIC)
                ))
            ))
        }

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isTrue
    }

    @Test
    fun legacyCategoryTakesPrecedenceOverConflictingProjectCategory() {
        val call = callWithFieldType("value.id", "NUMBER", PlSqlType.NUMERIC) { argument ->
            argument.plSqlDatatype = CharacterDatatype()
        }

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.CHARACTER).matches(call)).isTrue
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isFalse
    }

    @Test
    fun unknownEffectiveCategoryRetainsTheExistingMatcherPolicy() {
        val call = call("value.id")

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isFalse
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.UNKNOWN).matches(call)).isTrue
    }

    @Test
    fun projectUncertaintyRetainsTheExistingUnknownPolicy() {
        val reference = named("external_type")
        val anchoredReference = AnchoredTypeRef(
            QualifiedName(identifier("some_table")),
            TypeAnchor.TYPE,
            range()
        )
        val resolutions = listOf<TypeRefSemanticResolution>(
            TypeRefSemanticResolution.Project(
                reference,
                ProjectTypeResolution.Ambiguous(reference, listOf(record("first"), record("second")))
            ),
            TypeRefSemanticResolution.Project(
                reference,
                ProjectTypeResolution.IncompleteIndex(
                    reference,
                    listOf(record("known")),
                    listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
                )
            ),
            TypeRefSemanticResolution.Project(reference, ProjectTypeResolution.NotFoundInProject(reference)),
            TypeRefSemanticResolution.Unsupported(anchoredReference)
        )

        resolutions.forEach { resolution ->
            val call = call("value.id") { argument ->
                argument.projectRecordFieldTypeResolution = fieldTypeResolution(
                    field("id", resolution.reference),
                    resolution
                )
            }

            assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call))
                .describedAs("$resolution")
                .isFalse
            assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.UNKNOWN).matches(call))
                .describedAs("$resolution")
                .isTrue
        }
    }

    @Test
    fun stoppedProjectPathRetainsTheExistingUnknownPolicy() {
        val childReference = named("p.child")
        val child = field("child", childReference)
        val call = call("value.child.missing") { argument ->
            argument.projectRecordMemberPathResolution = ProjectRecordMemberPathResolution.Stopped(
                listOf(ProjectRecordMemberPathSegment(
                    child,
                    fieldTypeResolution(
                        child,
                        TypeRefSemanticResolution.Project(
                            childReference,
                            ProjectTypeResolution.NotFoundInProject(childReference)
                        )
                    )
                )),
                identifier("missing"),
                1,
                ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED
            )
        }

        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC).matches(call)).isFalse
        assertThat(MethodMatcher.create().name("func").addParameter(PlSqlType.UNKNOWN).matches(call)).isTrue
    }

    private fun call(expression: String, decorate: (SemanticAstNode) -> Unit = {}) =
        p.parse("func($expression)").firstChild.also { call ->
            decorate(MethodMatcher.semantic(MethodMatcher.create().getArguments(call).single().firstChild))
        }

    private fun callWithFieldType(
        expression: String,
        typeName: String,
        type: PlSqlType,
        decorate: (SemanticAstNode) -> Unit = {}
    ) = call(expression) { argument ->
        val typeRef = named(typeName)
        argument.projectRecordFieldTypeResolution = fieldTypeResolution(
            field("value", typeRef),
            TypeRefSemanticResolution.BuiltIn(typeRef, type)
        )
        decorate(argument)
    }

    private fun fieldTypeResolution(
        field: ProjectRecordField,
        resolution: TypeRefSemanticResolution
    ) = ProjectRecordFieldTypeResolution(field, resolution)

    private fun record(name: String) = PackageTypeDeclaration(
        QualifiedName(identifier("p")),
        identifier(name),
        ProjectTypeShape.RECORD,
        emptyList(),
        FileId("$name.sql"),
        range()
    )

    private fun field(name: String, typeRef: TypeRef) = ProjectRecordField(
        identifier(name),
        0,
        typeRef,
        range()
    )

    private fun named(name: String) = NamedTypeRef(
        QualifiedName(name.split('.').map(::identifier)),
        range()
    )

    private fun identifier(name: String) = OracleIdentifier.fromSource(name)

    private fun range() = SourceRange(FileId("test.sql"), 1, 0, 1, 1)

    private class MatcherCheck(private val matcher: MethodMatcher) : PlSqlCheck() {
        var matched = false
        var legacyArgumentType: PlSqlType? = null
        var projectTypeDeclaration: ProjectTypeDeclaration? = null

        init {
            subscribeTo(PlSqlGrammar.METHOD_CALL)
        }

        override fun visitNode(node: com.felipebz.flr.api.AstNode) {
            val argument = matcher.getArguments(node).single().firstChild
            val semanticArgument = MethodMatcher.semantic(argument)
            legacyArgumentType = semanticArgument.plSqlType
            projectTypeDeclaration = semanticArgument.symbol?.projectTypeDeclaration
            matched = matcher.matches(node)
        }
    }

    private class ProjectFieldMatcherCheck(private val matcher: MethodMatcher) : PlSqlCheck() {
        var matched = false
        var legacyArgumentType: PlSqlType? = null
        var projectField: ProjectRecordField? = null
        var effectiveCategory: EffectiveTypeCategory? = null

        init {
            subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
        }

        override fun visitNode(node: com.felipebz.flr.api.AstNode) {
            val member = node as? SemanticAstNode ?: return
            val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
            if (parts.map { it.tokenOriginalValue } != listOf("value", "id")) return
            val methodCall = node.getFirstAncestorOrNull(PlSqlGrammar.METHOD_CALL) ?: return
            legacyArgumentType = member.plSqlType
            projectField = member.projectRecordFieldTypeResolution?.field
            effectiveCategory = EffectiveSemanticTypeQuery.typeCategory(member)
            matched = matcher.matches(methodCall)
        }
    }

    private class FixtureFile(
        private val fileId: FileId,
        private val source: String
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = fileId.value
        override fun path(): Path = Path.of(fileId.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
