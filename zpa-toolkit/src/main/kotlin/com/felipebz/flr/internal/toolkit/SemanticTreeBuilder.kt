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

import com.felipebz.zpa.tooling.LegacySemanticSummary
import com.felipebz.zpa.tooling.ProjectDeclarationSummary
import com.felipebz.zpa.tooling.ProjectRecordFieldSummary
import com.felipebz.zpa.tooling.ProjectRecordFieldTypeResolutionState
import com.felipebz.zpa.tooling.ProjectRecordFieldTypeResolutionSummary
import com.felipebz.zpa.tooling.ProjectRecordMemberPathResolutionState
import com.felipebz.zpa.tooling.ProjectRecordMemberPathResolutionSummary
import com.felipebz.zpa.tooling.ProjectRecordMemberResolutionSummary
import com.felipebz.zpa.tooling.ProjectRecordMemberResolutionState
import com.felipebz.zpa.tooling.ProjectTypeResolutionState
import com.felipebz.zpa.tooling.ProjectTypeResolutionSummary
import com.felipebz.zpa.tooling.SemanticNodeInspection
import com.felipebz.zpa.tooling.SemanticSourceRange
import com.felipebz.zpa.tooling.SemanticTypeRefSummary
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/** Builds the concise, human-oriented semantic inspection tree for the toolkit. */
internal class SemanticTreeBuilder {
    fun build(inspection: SemanticNodeInspection): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode(nodeHeading(inspection))
        val pathResolution = inspection.projectRecordMemberPathResolution
        val memberResolution = inspection.projectRecordMemberResolution
        val fieldTypeResolution = inspection.projectRecordFieldTypeResolution
        val typeResolution = inspection.projectTypeResolution
        val symbolDeclaration = inspection.symbolProjectTypeDeclaration
        val hasProjectMetadata = hasProjectMetadata(inspection)
        val resolution = DefaultMutableTreeNode("Resolution")
        resolution.add(DefaultMutableTreeNode("source: ${resolutionSource(inspection, hasProjectMetadata)}"))

        when {
            pathResolution != null -> {
                resolution.add(DefaultMutableTreeNode("base: ${inspection.node.sourceValue ?: "<unknown>"}"))
                resolution.add(DefaultMutableTreeNode(
                    "path: ${pathState(pathResolution.state)}"
                ))
            }
            memberResolution != null -> {
                val member = memberResolution
                resolution.add(DefaultMutableTreeNode(
                    "member: ${member.field?.name ?: memberState(member.state)}"
                ))
                resolution.add(DefaultMutableTreeNode(
                    "base type: ${member.containingDeclaration.qualifiedName}"
                ))
                fieldTypeResolution?.let { fieldType ->
                    resolution.add(DefaultMutableTreeNode("member type: ${fieldType.typeRef.name}"))
                    resolution.add(DefaultMutableTreeNode(
                        "type resolution: ${fieldTypeState(fieldType)}"
                    ))
                }
            }
            typeResolution != null -> {
                val type = typeResolution
                resolution.add(DefaultMutableTreeNode("type: ${type.reference}"))
                resolution.add(DefaultMutableTreeNode(
                    "project declaration: ${projectTypeState(type.state)}"
                ))
            }
            symbolDeclaration != null -> {
                resolution.add(DefaultMutableTreeNode(
                    "symbol type: ${symbolDeclaration.qualifiedName}"
                ))
                resolution.add(DefaultMutableTreeNode("project declaration: Resolved"))
            }
            inspection.legacy != null -> resolution.add(DefaultMutableTreeNode("legacy result available"))
            else -> resolution.add(DefaultMutableTreeNode("project metadata: none"))
        }
        root.add(resolution)

        when {
            pathResolution != null -> root.add(getRecordMemberPathSummary(pathResolution))
            memberResolution?.field != null -> {
                val member = memberResolution
                val field = member.field!!
                root.add(getRecordDeclarationSummary(member.containingDeclaration, field))
                fieldTypeResolution?.let {
                    root.add(getRecordFieldTypeSummary(it))
                }
            }
            typeResolution?.declaration != null ->
                root.add(getDeclarationTreeNode("Declaration", typeResolution.declaration!!))
            symbolDeclaration != null -> root.add(getDeclarationTreeNode("Declaration", symbolDeclaration))
            !hasProjectMetadata && inspection.legacy != null ->
                root.add(getLegacySemanticTreeNode(inspection.legacy!!))
        }

        if (!hasSemanticInformation(inspection)) {
            root.add(DefaultMutableTreeNode("No semantic information for this node"))
        }

        val details = getDetailsTreeNode(inspection, hasProjectMetadata)
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
            if (label == "Resolution" || label == "Legacy semantics" ||
                label.startsWith("Declaration:") || label.startsWith("Type resolution") ||
                label.startsWith("RECORD path:")
            ) {
                tree.expandPath(TreePath(child.path))
                if (label.startsWith("RECORD path:")) {
                    for (segmentIndex in 0 until child.childCount) {
                        val segment = child.getChildAt(segmentIndex) as DefaultMutableTreeNode
                        if (segment.userObject.toString().startsWith("[")) {
                            tree.expandPath(TreePath(segment.path))
                        }
                    }
                }
            }
        }
    }

    private fun nodeHeading(inspection: SemanticNodeInspection): String {
        val pathResolution = inspection.projectRecordMemberPathResolution
        val memberResolution = inspection.projectRecordMemberResolution
        val typeResolution = inspection.projectTypeResolution
        val value = when {
            pathResolution != null -> {
                val path = pathResolution
                joinNodeParts(inspection.node.sourceValue, path.segments.map { it.field.name })
            }
            memberResolution?.field != null -> joinNodeParts(
                inspection.node.sourceValue,
                listOf(memberResolution.field!!.name)
            )
            typeResolution != null -> typeResolution.reference
            else -> inspection.node.sourceValue
        }
        return if (value.isNullOrBlank()) {
            "Node: ${inspection.node.grammarType}"
        } else {
            "${inspection.node.grammarType}: $value"
        }
    }

    private fun joinNodeParts(base: String?, parts: List<String>): String? {
        if (base.isNullOrBlank()) return parts.takeIf { it.isNotEmpty() }?.joinToString(".")
        return if (parts.isEmpty()) base else "$base.${parts.joinToString(".")}"
    }

    private fun hasProjectMetadata(inspection: SemanticNodeInspection): Boolean =
        inspection.projectTypeResolution != null ||
            inspection.symbolProjectTypeDeclaration != null ||
            inspection.projectRecordMemberResolution != null ||
            inspection.projectRecordFieldTypeResolution != null ||
            inspection.projectRecordMemberPathResolution != null

    private fun hasSemanticInformation(inspection: SemanticNodeInspection): Boolean =
        inspection.legacy != null || hasProjectMetadata(inspection)

    private fun resolutionSource(inspection: SemanticNodeInspection, hasProjectMetadata: Boolean): String = when {
        hasProjectMetadata -> "Project semantics"
        inspection.legacy != null -> "Legacy semantics"
        else -> "No semantic resolution"
    }

    private fun getRecordDeclarationSummary(
        declaration: ProjectDeclarationSummary,
        field: ProjectRecordFieldSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(
            "Declaration: ${declaration.shape ?: declaration.kind}: ${declaration.qualifiedName}"
        )
        node.add(DefaultMutableTreeNode("field: ${field.name}"))
        node.add(DefaultMutableTreeNode("ordinal: ${field.ordinal}"))
        return node
    }

    private fun getRecordFieldTypeSummary(
        resolution: ProjectRecordFieldTypeResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("Type resolution")
        node.add(DefaultMutableTreeNode(
            "${resolution.typeRef.name}: ${fieldTypeState(resolution)}"
        ))
        resolution.resolution?.let { projectResolution ->
            projectResolution.declaration?.let {
                node.add(DefaultMutableTreeNode("project declaration: ${it.qualifiedName}"))
            }
        }
        return node
    }

    private fun getRecordMemberPathSummary(
        resolution: ProjectRecordMemberPathResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("RECORD path: ${pathState(resolution.state)}")
        for (segment in resolution.segments) {
            val segmentNode = DefaultMutableTreeNode("[${segment.ordinal}] ${segment.field.name}")
            segmentNode.add(DefaultMutableTreeNode("type: ${segment.fieldTypeResolution.typeRef.name}"))
            segmentNode.add(DefaultMutableTreeNode(
                "project declaration: ${fieldTypeState(segment.fieldTypeResolution)}"
            ))
            node.add(segmentNode)
        }
        if (resolution.state == ProjectRecordMemberPathResolutionState.STOPPED) {
            node.add(DefaultMutableTreeNode("Stopped before: ${resolution.nextMember}"))
            node.add(DefaultMutableTreeNode("Reason: ${stopReason(resolution.stopReason)}"))
        }
        return node
    }

    private fun getDetailsTreeNode(
        inspection: SemanticNodeInspection,
        hasProjectMetadata: Boolean
    ): DefaultMutableTreeNode {
        val details = DefaultMutableTreeNode("Details")
        val node = DefaultMutableTreeNode("Node metadata")
        inspection.node.sourceValue?.let { node.add(DefaultMutableTreeNode("source value: $it")) }
        inspection.node.sourceRange?.let { node.add(DefaultMutableTreeNode("range: ${formatRange(it)}")) }
        if (node.childCount > 0) details.add(node)

        val legacy = inspection.legacy
        if (hasProjectMetadata && legacy != null) {
            details.add(getLegacySemanticTreeNode(legacy, "Legacy semantics (raw)"))
        }
        inspection.projectTypeResolution?.let {
            details.add(getProjectTypeResolutionTreeNode("Project type resolution", it))
        }
        inspection.symbolProjectTypeDeclaration?.let {
            details.add(getDeclarationTreeNode("Symbol project type declaration", it))
        }
        inspection.projectRecordMemberResolution?.let {
            details.add(getRecordMemberResolutionTreeNode("Direct RECORD member", it))
        }
        inspection.projectRecordFieldTypeResolution?.let {
            details.add(getRecordFieldTypeResolutionTreeNode(it))
        }
        inspection.projectRecordMemberPathResolution?.let {
            details.add(getRecordMemberPathResolutionTreeNode(it))
        }
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
        label: String = "Legacy semantics"
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

    private fun getProjectTypeResolutionTreeNode(
        label: String,
        resolution: ProjectTypeResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("$label: ${projectTypeState(resolution.state)}")
        node.add(DefaultMutableTreeNode("raw state: ${resolution.state}"))
        node.add(DefaultMutableTreeNode("reference: ${resolution.reference}"))
        resolution.declaration?.let { node.add(getDeclarationTreeNode("declaration", it)) }
        if (resolution.candidates.isNotEmpty()) {
            val candidates = DefaultMutableTreeNode("candidates")
            resolution.candidates.forEach { candidates.add(getDeclarationTreeNode("candidate", it)) }
            node.add(candidates)
        }
        if (resolution.knownCandidates.isNotEmpty()) {
            val candidates = DefaultMutableTreeNode("known candidates")
            resolution.knownCandidates.forEach { candidates.add(getDeclarationTreeNode("candidate", it)) }
            node.add(candidates)
        }
        if (resolution.preparationFailures.isNotEmpty()) {
            val failures = DefaultMutableTreeNode("preparation failures")
            resolution.preparationFailures.forEach {
                failures.add(DefaultMutableTreeNode("${it.fileId}: ${it.exceptionType}"))
            }
            node.add(failures)
        }
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

    private fun getRecordMemberResolutionTreeNode(
        label: String,
        resolution: ProjectRecordMemberResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("$label: ${memberState(resolution.state)}")
        node.add(DefaultMutableTreeNode("raw state: ${resolution.state}"))
        node.add(getDeclarationTreeNode("containing declaration", resolution.containingDeclaration))
        resolution.field?.let { node.add(getRecordFieldTreeNode("field", it)) }
        if (resolution.candidates.isNotEmpty()) {
            val candidates = DefaultMutableTreeNode("candidates")
            resolution.candidates.forEach { candidates.add(getRecordFieldTreeNode("field", it)) }
            node.add(candidates)
        }
        return node
    }

    private fun getRecordFieldTypeResolutionTreeNode(
        resolution: ProjectRecordFieldTypeResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(
            "Direct RECORD field type: ${fieldTypeState(resolution)}"
        )
        node.add(DefaultMutableTreeNode("raw state: ${resolution.state}"))
        node.add(getRecordFieldTreeNode("field", resolution.field))
        node.add(getTypeRefTreeNode("type reference", resolution.typeRef))
        resolution.resolution?.let {
            node.add(getProjectTypeResolutionTreeNode("project type resolution", it))
        }
        return node
    }

    private fun getRecordMemberPathResolutionTreeNode(
        resolution: ProjectRecordMemberPathResolutionSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("RECORD member path: ${pathState(resolution.state)}")
        node.add(DefaultMutableTreeNode("raw state: ${resolution.state}"))
        for (segment in resolution.segments) {
            val segmentNode = DefaultMutableTreeNode("[${segment.ordinal}] ${segment.field.name}")
            segmentNode.add(getRecordFieldTreeNode("field", segment.field))
            val fieldType = DefaultMutableTreeNode(
                "field type: ${fieldTypeState(segment.fieldTypeResolution)}"
            )
            fieldType.add(DefaultMutableTreeNode("raw state: ${segment.fieldTypeResolution.state}"))
            fieldType.add(getTypeRefTreeNode("type reference", segment.fieldTypeResolution.typeRef))
            segment.fieldTypeResolution.resolution?.let {
                fieldType.add(getProjectTypeResolutionTreeNode("project type resolution", it))
            }
            segmentNode.add(fieldType)
            node.add(segmentNode)
        }
        if (resolution.state == ProjectRecordMemberPathResolutionState.STOPPED) {
            node.add(DefaultMutableTreeNode("next member: ${resolution.nextMember}"))
            node.add(DefaultMutableTreeNode("next member ordinal: ${resolution.nextMemberOrdinal}"))
            node.add(DefaultMutableTreeNode("stop reason: ${resolution.stopReason}"))
        }
        return node
    }

    private fun getRecordFieldTreeNode(label: String, field: ProjectRecordFieldSummary): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("$label: ${field.name}")
        node.add(DefaultMutableTreeNode("field ordinal: ${field.ordinal}"))
        node.add(getTypeRefTreeNode("field type", field.typeRef))
        node.add(DefaultMutableTreeNode("range: ${formatRange(field.sourceRange)}"))
        return node
    }

    private fun getTypeRefTreeNode(
        label: String,
        typeRef: SemanticTypeRefSummary
    ): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode("$label: ${typeRef.kind} ${typeRef.name}")
        typeRef.anchor?.let { node.add(DefaultMutableTreeNode("anchor: $it")) }
        node.add(DefaultMutableTreeNode("range: ${formatRange(typeRef.sourceRange)}"))
        return node
    }

    private fun formatRange(range: SemanticSourceRange): String =
        "${range.fileId}:${range.startLine}:${range.startColumn}-${range.endLine}:${range.endColumn}"

    private fun projectTypeState(state: ProjectTypeResolutionState): String = when (state) {
        ProjectTypeResolutionState.RESOLVED -> "Resolved"
        ProjectTypeResolutionState.AMBIGUOUS -> "Ambiguous"
        ProjectTypeResolutionState.NOT_FOUND_IN_PROJECT -> "No project declaration"
        ProjectTypeResolutionState.INCOMPLETE_INDEX -> "Incomplete project index"
        ProjectTypeResolutionState.NOT_PREPARED -> "Project index not prepared"
    }

    private fun fieldTypeState(resolution: ProjectRecordFieldTypeResolutionSummary): String = when {
        resolution.state == ProjectRecordFieldTypeResolutionState.UNSUPPORTED -> "Unsupported reference form"
        resolution.resolution == null -> "No project resolution"
        else -> projectTypeState(resolution.resolution!!.state)
    }

    private fun memberState(state: ProjectRecordMemberResolutionState): String = when (state) {
        ProjectRecordMemberResolutionState.RESOLVED -> "Resolved"
        ProjectRecordMemberResolutionState.NOT_FOUND -> "No matching project field"
        ProjectRecordMemberResolutionState.AMBIGUOUS -> "Ambiguous"
        ProjectRecordMemberResolutionState.UNSUPPORTED_TYPE -> "Unsupported type"
    }

    private fun pathState(state: ProjectRecordMemberPathResolutionState): String = when (state) {
        ProjectRecordMemberPathResolutionState.COMPLETED -> "Completed"
        ProjectRecordMemberPathResolutionState.STOPPED -> "Stopped"
    }

    private fun stopReason(reason: String?): String = when (reason) {
        "MEMBER_NOT_FOUND" -> "Member not found"
        "MEMBER_AMBIGUOUS" -> "Ambiguous member"
        "UNSUPPORTED_TYPE" -> "Unsupported type"
        "FIELD_TYPE_UNRESOLVED" -> "Field type unresolved"
        null -> "Unknown"
        else -> reason.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}
