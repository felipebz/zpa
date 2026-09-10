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
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.tooling.ProjectAnalysisSummary
import com.felipebz.zpa.tooling.ProjectTypeResolutionState
import com.felipebz.zpa.tooling.ProjectTypeResolutionSummary
import com.felipebz.zpa.tooling.SemanticNodeInspection
import com.felipebz.zpa.tooling.SemanticNodeSummary
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class SemanticTreeBuilderTest {
    private val builder = SemanticTreeBuilder()

    @Test
    fun directProjectRecordMemberIsPresentedAsOneConciseExplanation() {
        val model = model(simpleRecordSource())
        val inspection = model.inspect(model.findMember(listOf("value", "id")))!!
        val tree = builder.build(inspection)

        assertThat(tree.userObject).isEqualTo("MEMBER_EXPRESSION: value.id")
        assertThat(tree.topLevelLabels()).contains(
            "Resolution",
            "Declaration: RECORD: p.t",
            "Type resolution"
        )
        assertThat(tree.topLevelLabels()).noneMatch { it.startsWith("Direct RECORD") }
        assertThat(tree.labels()).contains(
            "source: Project semantics",
            "base type: p.t",
            "member: id",
            "member type: NUMBER",
            "NUMBER: Built-in NUMERIC",
            "Project context: 1 file · 2 declarations"
        )
        assertThat(tree.labels().count { it.startsWith("Direct RECORD") }).isEqualTo(2)
    }

    @Test
    fun legacyResolutionIsNamedWithoutProjectMetadata() {
        val model = model(
            """
            DECLARE
              TYPE local_rec IS RECORD (id NUMBER);
              value local_rec;
            BEGIN
              value.id := 1;
            END;
            """.trimIndent()
        )
        val value = model.findNode(PlSqlGrammar.IDENTIFIER_NAME) {
            it.tokenOriginalValue.equals("value", ignoreCase = true) &&
                it.parentOrNull?.type === PlSqlGrammar.VARIABLE_DECLARATION
        }
        val tree = builder.build(model.inspect(value)!!)

        assertThat(tree.labels()).contains(
            "source: Legacy semantics",
            "Legacy semantics",
            "legacy result available"
        )
        assertThat(tree.labels()).doesNotContain("project metadata: none")
    }

    @Test
    fun completedRecordPathIsShownAsAnOrderedTrace() {
        val model = model(projectRecordSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer", "address", "city")))!!
        val tree = builder.build(inspection)

        assertThat(tree.userObject).isEqualTo("MEMBER_EXPRESSION: value.customer.address.city")
        assertThat(tree.labels()).contains(
            "RECORD path: Completed",
            "[0] customer",
            "[1] address",
            "[2] city",
            "type: VARCHAR2",
            "semantic type: Built-in CHARACTER"
        )
    }

    @Test
    fun stoppedRecordPathShowsPrefixAndHumanReadableReason() {
        val model = model(projectRecordSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer", "address", "missing")))!!
        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains(
            "RECORD path: Stopped",
            "[0] customer",
            "[1] address",
            "Stopped before: missing",
            "Reason: Member not found"
        )
        assertThat(tree.labels()).doesNotContain("Reason: MEMBER_NOT_FOUND")
    }

    @Test
    fun ambiguousResolutionKeepsReadableStateAndDetailedCandidates() {
        val model = model(
            """
            DECLARE value p.t; BEGIN NULL; END;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
            """.trimIndent()
        )
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) {
            it.tokenText().replace(" ", "").contains("p.t")
        }
        val tree = builder.build(model.inspect(datatype)!!)

        assertThat(tree.labels()).contains(
            "project declaration: Ambiguous",
            "Project type resolution: Ambiguous",
            "raw state: AMBIGUOUS",
            "candidates"
        )
    }

    @Test
    fun incompleteProjectContextIsPromotedButItsDetailsRemainAvailable() {
        val failure = com.felipebz.zpa.tooling.ProjectPreparationFailureSummary(
            "broken.sql",
            "IllegalStateException"
        )
        val resolution = ProjectTypeResolutionSummary(
            ProjectTypeResolutionState.INCOMPLETE_INDEX,
            "p.t",
            null,
            emptyList(),
            emptyList(),
            listOf(failure)
        )
        val inspection = SemanticNodeInspection(
            node = SemanticNodeSummary("DATATYPE", "p", null),
            projectAnalysis = ProjectAnalysisSummary(
                "PREPARED_WITH_FAILURES",
                attemptedFileCount = 2,
                successfulFileCount = 1,
                declarationCount = 1,
                failures = listOf(failure)
            ),
            legacy = null,
            projectTypeResolution = resolution,
            symbolProjectTypeDeclaration = null,
            projectRecordMemberResolution = null,
            projectRecordFieldTypeResolution = null,
            projectRecordMemberPathResolution = null
        )
        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains(
            "Project context: incomplete · 1 preparation failure",
            "raw state: PREPARED_WITH_FAILURES",
            "preparation failures",
            "Project type resolution: Incomplete project index",
            "raw state: INCOMPLETE_INDEX"
        )
    }

    @Test
    fun emptyInspectionKeepsTheCleanEmptyState() {
        val inspection = SemanticNodeInspection(
            node = SemanticNodeSummary("IDENTIFIER_NAME", "value", null),
            projectAnalysis = ProjectAnalysisSummary("PREPARED_EMPTY", 0, 0, 0, emptyList()),
            legacy = null,
            projectTypeResolution = null,
            symbolProjectTypeDeclaration = null,
            projectRecordMemberResolution = null,
            projectRecordFieldTypeResolution = null,
            projectRecordMemberPathResolution = null
        )

        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains(
            "No semantic information for this node",
            "Project context: 0 files · 0 declarations"
        )
    }

    @Test
    fun primarySectionsAreExpandedWhileDetailsRemainCollapsed() {
        val model = model(projectRecordSource())
        val root = builder.build(
            model.inspect(model.findMember(listOf("value", "customer", "address", "city")))!!
        )
        val tree = JTree(root)

        builder.expandPrimary(tree)

        assertThat(tree.isExpanded(TreePath(root.path))).isTrue()
        assertThat(tree.isExpanded(TreePath(root.child("Resolution").path))).isTrue()
        val path = root.child("RECORD path: Completed")
        assertThat(tree.isExpanded(TreePath(path.path))).isTrue()
        assertThat(tree.isExpanded(TreePath(path.child("[0] customer").path))).isTrue()
        assertThat(tree.isExpanded(TreePath(root.child("Details").path))).isFalse()
    }

    private fun model(source: String): SourceCodeModel = SourceCodeModel(ZpaConfigurationModel()).apply {
        setSourceCode(source)
    }

    private fun SourceCodeModel.findMember(parts: List<String>): AstNode =
        astNode.descendantsAndSelf().single {
            it.type === PlSqlGrammar.MEMBER_EXPRESSION &&
                it.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
                    .map { child -> child.tokenOriginalValue } == parts
        }

    private fun SourceCodeModel.findNode(
        type: com.felipebz.flr.api.AstNodeType,
        predicate: (AstNode) -> Boolean
    ): AstNode = astNode.descendantsAndSelf().single { it.type === type && predicate(it) }

    private fun AstNode.tokenText() = tokens.joinToString(" ") { it.originalValue }

    private fun AstNode.descendantsAndSelf(): Sequence<AstNode> = sequence {
        yield(this@descendantsAndSelf)
        for (child in children) yieldAll(child.descendantsAndSelf())
    }

    private fun DefaultMutableTreeNode.labels(): List<String> = buildList {
        add(userObject.toString())
        for (index in 0 until childCount) {
            addAll((getChildAt(index) as DefaultMutableTreeNode).labels())
        }
    }

    private fun DefaultMutableTreeNode.topLevelLabels(): List<String> =
        (0 until childCount).map { getChildAt(it).toString() }

    private fun DefaultMutableTreeNode.child(label: String): DefaultMutableTreeNode =
        (0 until childCount)
            .map { getChildAt(it) as DefaultMutableTreeNode }
            .single { it.userObject == label }

    private fun projectRecordSource() =
        """
        CREATE PACKAGE p AS
          TYPE t IS RECORD (id NUMBER);
          TYPE city_rec IS RECORD (city VARCHAR2(100));
          TYPE address_rec IS RECORD (address city_rec);
          TYPE customer_rec IS RECORD (customer address_rec);
        END p;
        DECLARE
          value p.customer_rec;
        BEGIN
          value.id := 1;
          value.customer.address.city := NULL;
          value.customer.address.missing := NULL;
        END;
        """.trimIndent()

    private fun simpleRecordSource() =
        """
        CREATE PACKAGE p AS
          TYPE t IS RECORD (id NUMBER);
        END p;
        DECLARE
          value p.t;
        BEGIN
          value.id := 1;
        END;
        """.trimIndent()
}
