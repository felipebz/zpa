//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.zpa.tooling.*
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/** Builds the concise, human-oriented semantic inspection tree for the toolkit. */
internal class SemanticTreeBuilder {
    fun build(inspection: SemanticNodeInspection): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode(nodeHeading(inspection))
        val typeName = inspection.typeName
        val fields = inspection.fields
        val declaredType = inspection.declaredProjectType
        val hasLocalAnswer = hasLocalAnswer(inspection)
        val resolution = DefaultMutableTreeNode("Resolution")
        resolution.add(DefaultMutableTreeNode("source: ${resolutionSource(inspection, hasLocalAnswer)}"))

        when {
            typeName != null -> {
                resolution.add(DefaultMutableTreeNode("type: ${typeName.reference}"))
                resolution.add(DefaultMutableTreeNode("declaration: ${typeNameState(typeName)}"))
                inspection.typeCategory?.let { resolution.add(DefaultMutableTreeNode("category: $it")) }
            }
            fields.isNotEmpty() -> {
                fields.forEach { resolution.add(DefaultMutableTreeNode("${it.name}: ${fieldState(it)}")) }
                inspection.valueCategory?.let { resolution.add(DefaultMutableTreeNode("category: $it")) }
            }
            inspection.valueCategory != null -> resolution.add(DefaultMutableTreeNode("category: ${inspection.valueCategory}"))
            declaredType != null -> {
                resolution.add(DefaultMutableTreeNode("declared type: ${declaredType.qualifiedName}"))
                resolution.add(DefaultMutableTreeNode("project declaration: Resolved"))
            }
            inspection.legacy != null -> resolution.add(DefaultMutableTreeNode("legacy result available"))
            else -> resolution.add(DefaultMutableTreeNode("project metadata: none"))
        }
        root.add(resolution)

        when {
            typeName?.declaration != null -> root.add(getDeclarationTreeNode("Declaration", typeName.declaration!!))
            declaredType != null -> root.add(getDeclarationTreeNode("Declaration", declaredType))
            !hasLocalAnswer && inspection.legacy != null -> root.add(getLegacySemanticTreeNode(inspection.legacy!!))
        }

        if (!hasSemanticInformation(inspection)) {
            root.add(DefaultMutableTreeNode("No semantic information for this node"))
        }

        val details = getDetailsTreeNode(inspection, hasLocalAnswer)
        if (details.childCount > 0) root.add(details)
        root.add(getProjectContextTreeNode(inspection))
        return root
    }

    /** Expands only the root and concise explanation sections, leaving Details collapsed. */
    fun expandPrimary(tree: JTree) {
        val root = tree.model.root as? DefaultMutableTreeNode ?: return
        tree.expandPath(TreePath(root.path))
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index) as DefaultMutableTreeNode
            val label = child.userObject.toString()
            if (label == "Resolution" || label == LEGACY_SEMANTICS || label.startsWith("Declaration:")) {
                tree.expandPath(TreePath(child.path))
            }
        }
    }

    private fun nodeHeading(inspection: SemanticNodeInspection): String {
        val value = inspection.typeName?.reference ?: memberText(inspection) ?: inspection.node.sourceValue
        return if (value.isNullOrBlank()) "Node: ${inspection.node.grammarType}" else "${inspection.node.grammarType}: $value"
    }

    /** The names of a member node: its first name and the names after it, or only these when they start the node. */
    private fun memberText(inspection: SemanticNodeInspection): String? {
        val fields = inspection.fields.takeIf { it.isNotEmpty() } ?: return null
        val names = fields.joinToString(".") { it.name }
        val start = inspection.node.sourceRange ?: return names
        val first = fields.first().sourceRange
        return if (first.startLine == start.startLine && first.startColumn == start.startColumn) names else "${inspection.node.sourceValue}.$names"
    }

    private fun hasLocalAnswer(inspection: SemanticNodeInspection): Boolean =
        inspection.typeName != null || inspection.typeCategory != null || inspection.valueCategory != null ||
            inspection.declaredProjectType != null || inspection.fields.isNotEmpty()

    private fun hasSemanticInformation(inspection: SemanticNodeInspection): Boolean =
        inspection.legacy != null || hasLocalAnswer(inspection)

    private fun resolutionSource(inspection: SemanticNodeInspection, hasLocalAnswer: Boolean): String = when {
        hasLocalAnswer -> "Local semantics"
        inspection.legacy != null -> LEGACY_SEMANTICS
        else -> "No semantic resolution"
    }

    private fun getDetailsTreeNode(inspection: SemanticNodeInspection, hasLocalAnswer: Boolean): DefaultMutableTreeNode {
        val details = DefaultMutableTreeNode("Details")
        val node = DefaultMutableTreeNode("Node metadata")
        inspection.node.sourceValue?.let { node.add(DefaultMutableTreeNode("source value: $it")) }
        inspection.node.sourceRange?.let { node.add(DefaultMutableTreeNode("range: ${formatRange(it)}")) }
        if (node.childCount > 0) details.add(node)

        val legacy = inspection.legacy
        if (hasLocalAnswer && legacy != null) {
            details.add(getLegacySemanticTreeNode(legacy, "Legacy semantics (raw)"))
        }
        inspection.typeName?.let { details.add(getTypeNameTreeNode(it)) }
        inspection.declaredProjectType?.let { details.add(getDeclarationTreeNode("Declared project type", it)) }
        inspection.fields.forEach { details.add(getFieldTreeNode(it)) }
        return details
    }

    private fun getProjectContextTreeNode(inspection: SemanticNodeInspection): DefaultMutableTreeNode {
        val project = inspection.projectAnalysis
        val context = if (project.failures.isNotEmpty()) {
            "Project context: incomplete · ${project.failures.size} preparation failure${if (project.failures.size == 1) "" else "s"}"
        } else {
            "Project context: ${project.attemptedFileCount} file${if (project.attemptedFileCount == 1) "" else "s"} · " +
                "${project.declarationCount} declaration${if (project.declarationCount == 1) "" else "s"}"
        }
        val node = DefaultMutableTreeNode(context)
        node.add(DefaultMutableTreeNode("raw state: ${project.state}"))
        node.add(DefaultMutableTreeNode("attempted files: ${project.attemptedFileCount}"))
        node.add(DefaultMutableTreeNode("successful files: ${project.successfulFileCount}"))
        node.add(DefaultMutableTreeNode("declarations: ${project.declarationCount}"))
        if (project.failures.isNotEmpty()) {
            val failures = DefaultMutableTreeNode("preparation failures")
            project.failures.forEach {
                failures.add(DefaultMutableTreeNode("${it.fileId}: ${it.exceptionType}"))
            }
            node.add(failures)
        }
        return node
    }

    private fun getLegacySemanticTreeNode(
        legacy: LegacySemanticSummary,
        label: String = LEGACY_SEMANTICS
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(label)
        legacy.symbol?.let {
            node.add(DefaultMutableTreeNode("symbol: ${it.name} · ${it.kind}"))
            node.add(DefaultMutableTreeNode("type: ${it.type}"))
            node.add(DefaultMutableTreeNode("datatype: ${it.datatype}"))
        }
        legacy.type?.let { node.add(DefaultMutableTreeNode("type: $it")) }
        legacy.datatype?.let { node.add(DefaultMutableTreeNode("datatype: $it")) }
        return node
    }

    private fun getTypeNameTreeNode(typeName: TypeNameSummary): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("Type name: ${typeNameState(typeName)}")
        node.add(DefaultMutableTreeNode("raw state: ${typeName.state}"))
        node.add(DefaultMutableTreeNode("reference: ${typeName.reference}"))
        typeName.declaration?.let { node.add(getDeclarationTreeNode("declaration", it)) }
        typeName.localDeclaration?.let { node.add(DefaultMutableTreeNode("local declaration: ${formatRange(it)}")) }
        if (typeName.reasons.isNotEmpty()) node.add(DefaultMutableTreeNode("reasons: ${typeName.reasons.joinToString(", ")}"))
        return node
    }

    private fun getFieldTreeNode(field: FieldBindingSummary): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("Field ${field.name}: ${fieldState(field)}")
        node.add(DefaultMutableTreeNode("raw state: ${field.state}"))
        node.add(DefaultMutableTreeNode("range: ${formatRange(field.sourceRange)}"))
        field.kind?.let { node.add(DefaultMutableTreeNode("kind: $it")) }
        field.ordinal?.let { node.add(DefaultMutableTreeNode("ordinal: $it")) }
        field.declaration?.let { node.add(DefaultMutableTreeNode("declaration: ${formatRange(it)}")) }
        if (field.reasons.isNotEmpty()) node.add(DefaultMutableTreeNode("reasons: ${field.reasons.joinToString(", ")}"))
        return node
    }

    private fun getDeclarationTreeNode(label: String, declaration: ProjectDeclarationSummary): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("$label: ${declaration.qualifiedName}")
        node.add(DefaultMutableTreeNode("kind: ${declaration.kind}"))
        node.add(DefaultMutableTreeNode("role: ${declaration.role}"))
        declaration.shape?.let { node.add(DefaultMutableTreeNode("shape: $it")) }
        node.add(DefaultMutableTreeNode("file: ${declaration.fileId}"))
        node.add(DefaultMutableTreeNode("range: ${formatRange(declaration.sourceRange)}"))
        return node
    }

    private fun formatRange(range: SemanticSourceRange): String =
        "${range.fileId}:${range.startLine}:${range.startColumn}-${range.endLine}:${range.endColumn}"

    private fun typeNameState(typeName: TypeNameSummary): String = when (typeName.state) {
        TypeNameState.LOCAL -> "Local declaration"
        TypeNameState.PROJECT -> "Resolved ${typeName.declaration?.qualifiedName.orEmpty()}".trimEnd()
        TypeNameState.BUILT_IN -> "Predefined type"
        TypeNameState.AMBIGUOUS -> "Ambiguous"
        TypeNameState.NOT_FOUND_IN_PROJECT -> "No project declaration"
        TypeNameState.INCOMPLETE_INDEX -> "Incomplete project index"
        TypeNameState.NOT_PREPARED -> "Project index not prepared"
        TypeNameState.UNDECIDED -> "Undecided"
    }

    private fun fieldState(field: FieldBindingSummary): String = when (field.state) {
        FieldBindingState.FIELD -> "${if (field.kind == "ATTRIBUTE") "attribute" else "field"} #${field.ordinal}"
        FieldBindingState.UNKNOWN -> "Undecided"
        FieldBindingState.NOT_A_FIELD -> "Not a field"
    }
}

private const val LEGACY_SEMANTICS = "Legacy semantics"
