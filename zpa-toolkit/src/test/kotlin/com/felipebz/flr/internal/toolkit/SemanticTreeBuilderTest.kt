//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.tooling.*
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class SemanticTreeBuilderTest {
    private val builder = SemanticTreeBuilder()

    @Test
    fun aMemberOfAProjectRecordIsPresentedAsOneConciseExplanation() {
        val model = model(simpleRecordSource())
        val inspection = model.inspect(model.findMember(listOf("value", "id")))!!
        val tree = builder.build(inspection)

        assertThat(tree.userObject).isEqualTo("MEMBER_EXPRESSION: value.id")
        assertThat(tree.topLevelLabels()).contains("Resolution", "Details")
        assertThat(tree.labels()).contains(
            "source: Local semantics",
            "id: field #0",
            "category: Scalar(NUMERIC)",
            "Field id: field #0",
            "Project context: 1 file · 2 declarations"
        )
    }

    @Test
    fun legacyResolutionIsNamedWithoutLocalAnswers() {
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
    fun aMemberChainShowsTheFieldOfEachName() {
        val model = model(projectRecordSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer", "address", "missing")))!!
        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains("customer: field #0", "address: field #0", "missing: Undecided")
    }

    @Test
    fun aTypeNameShowsItsDeclarationAndCategory() {
        val model = model(simpleRecordSource())
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) { it.tokenText().replace(" ", "") == "p.t" }
        val tree = builder.build(model.inspect(datatype)!!)

        assertThat(tree.userObject).isEqualTo("DATATYPE: p.t")
        assertThat(tree.topLevelLabels()).contains("Declaration: p.t")
        assertThat(tree.labels()).contains("declaration: Resolved p.t", "category: Record(RECORD_TYPE)", "Type name: Resolved p.t", "raw state: PROJECT")
    }

    @Test
    fun anUndecidedTypeNameShowsItsReasons() {
        val inspection = SemanticNodeInspection(
            node = SemanticNodeSummary("DATATYPE", "p", null),
            projectAnalysis = ProjectAnalysisSummary("PREPARED_WITH_DECLARATIONS", 1, 1, 1, emptyList()),
            legacy = null,
            typeName = TypeNameSummary(TypeNameState.UNDECIDED, "p.t", null, null, listOf("CONDITIONAL_COMPILATION")),
            typeCategory = "Unknown[CONDITIONAL_COMPILATION]",
            valueCategory = null,
            declaredProjectType = null,
            fields = emptyList()
        )
        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains("declaration: Undecided", "Type name: Undecided", "reasons: CONDITIONAL_COMPILATION")
    }

    @Test
    fun incompleteProjectContextIsPromotedButItsDetailsRemainAvailable() {
        val failure = ProjectPreparationFailureSummary("broken.sql", "IllegalStateException")
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
            typeName = TypeNameSummary(TypeNameState.INCOMPLETE_INDEX, "p.t", null, null, emptyList()),
            typeCategory = null,
            valueCategory = null,
            declaredProjectType = null,
            fields = emptyList()
        )
        val tree = builder.build(inspection)

        assertThat(tree.labels()).contains(
            "Project context: incomplete · 1 preparation failure",
            "raw state: PREPARED_WITH_FAILURES",
            "preparation failures",
            "Type name: Incomplete project index",
            "raw state: INCOMPLETE_INDEX"
        )
    }

    @Test
    fun emptyInspectionKeepsTheCleanEmptyState() {
        val inspection = SemanticNodeInspection(
            node = SemanticNodeSummary("IDENTIFIER_NAME", "value", null),
            projectAnalysis = ProjectAnalysisSummary("PREPARED_EMPTY", 0, 0, 0, emptyList()),
            legacy = null,
            typeName = null,
            typeCategory = null,
            valueCategory = null,
            declaredProjectType = null,
            fields = emptyList()
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
