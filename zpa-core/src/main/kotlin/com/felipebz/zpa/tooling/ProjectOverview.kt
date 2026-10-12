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
package com.felipebz.zpa.tooling

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectCallTarget
import com.felipebz.zpa.api.project.ProjectDeclarationKind
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.project.*

/** One labelled entry of the project overview; [astNode] is set only for resolutions attached to AST nodes. */
public class ProjectOverviewNode(
    public val label: String,
    public val sourceRange: SemanticSourceRange?,
    public val astNode: AstNode?,
    children: List<ProjectOverviewNode> = emptyList()
) {
    public val children: List<ProjectOverviewNode> = children.toList()

    override fun toString(): String = label
}

/** Builds the overview of everything the Project System captured for one analyzed file. */
@OptIn(ZpaExperimentalApi::class)
internal class ProjectOverviewBuilder(
    private val fileId: FileId,
    private val root: AstNode,
    private val symbolTable: SymbolTable,
    context: ProjectAnalysisContext
) {
    private val snapshot = ProjectSnapshot.create(context)
    private val declarations = snapshot.declarations
    private val fileDeclarations = declarations.filter { it.fileId == fileId.value }

    fun build(): ProjectOverviewNode = ProjectOverviewNode(
        "Project overview",
        null,
        null,
        listOfNotNull(
            preparation(),
            projectFiles(),
            packages(),
            section("Standalone types", fileDeclarations
                .filter { it.kind == ProjectDeclarationKind.STANDALONE_TYPE }
                .map { shapedTypeNode(it as ProjectShapedTypeView, it.qualifiedName.toString(), null, emptyList()) }),
            section("Standalone procedures and functions", fileDeclarations
                .filter {
                    it.kind == ProjectDeclarationKind.STANDALONE_PROCEDURE ||
                        it.kind == ProjectDeclarationKind.STANDALONE_FUNCTION
                }
                .map { subprogramNode(it as ProjectSubprogramView, it.qualifiedName.toString(), null) }),
            section("Sequences", fileDeclarations
                .filter { it.kind == ProjectDeclarationKind.SEQUENCE }
                .map { node("SEQUENCE ${it.qualifiedName}${at(it.sourceRange)}", range(it.sourceRange)) }),
            section("Omitted standalone callables", snapshot.projectIndex.omittedStandaloneCallables
                .sortedBy { it.toString() }
                .map {
                    node("$it · written-name discovery: " +
                        discovery(ProjectCallTarget.written(ProjectQualifiedName.wrap(it))))
                }),
            resolutions()
        )
    )

    private fun preparation() = node(
        "Preparation: ${snapshot.preparationState} · " +
            "${snapshot.successfulFileCount}/${snapshot.attemptedFileCount} files · " +
            "${declarations.size} declarations",
        children = snapshot.failures.map { node("failure: ${it.fileId}: ${it.exceptionType}") }
    )

    private fun projectFiles(): ProjectOverviewNode? = section("Project files", snapshot.fileIds
        .filter { it != fileId.value }
        .map { id ->
            val views = declarations.filter { it.fileId == id }
            val failure = snapshot.failures.firstOrNull { it.fileId == id }
            node(
                if (failure != null) "$id · failed: ${failure.exceptionType}" else "$id · ${views.size} declarations",
                children = views.map { node("${it.kind} ${it.qualifiedName}${at(it.sourceRange)}", range(it.sourceRange)) }
            )
        })

    private class Group(
        val range: SemanticSourceRange?,
        val header: List<ProjectOverviewNode>,
        val label: (memberCount: Int) -> String
    ) {
        val members = mutableListOf<ProjectOverviewNode>()
    }

    private data class BodyKey(val fileId: String, val owner: ProjectQualifiedName)

    private data class OrphanKey(val fileId: String, val owner: ProjectQualifiedName)

    private fun packages(): ProjectOverviewNode? {
        val groups = LinkedHashMap<Any, Group>()
        val specifications = mutableListOf<Pair<ProjectDeclarationView, Group>>()
        for (view in fileDeclarations) {
            if (view.kind == ProjectDeclarationKind.PACKAGE) {
                val group = Group(range(view.sourceRange), listOfNotNull(coverageNode(view))) {
                    "PACKAGE ${view.qualifiedName} (specification)${at(view.sourceRange)}"
                }
                groups[view] = group
                specifications += view to group
                continue
            }
            if (view is ProjectPackageSubprogramView && view.role == ProjectDeclarationRole.BODY) {
                val owner = view.packageOwner
                groups.getOrPut(BodyKey(view.fileId, owner)) {
                    Group(null, emptyList()) { count -> "PACKAGE BODY $owner · $count subprogram(s)" }
                }.members += subprogramNode(view, view.localName.toString(), owner)
                continue
            }
            val owner = memberOwner(view) ?: continue
            val container = specifications.firstOrNull { (specification, _) ->
                specification.qualifiedName == owner && specification.fileId == view.fileId &&
                    contains(specification.sourceRange, view.sourceRange)
            }?.second
            val group = container ?: groups.getOrPut(OrphanKey(view.fileId, owner)) {
                Group(null, emptyList()) { "PACKAGE $owner (members without specification fact)" }
            }
            group.members += memberNode(view, owner)
        }
        if (groups.isEmpty()) return null
        return node(
            "Packages (${groups.size})",
            children = groups.values.map { node(it.label(it.members.size), it.range, children = it.header + it.members) }
        )
    }

    private fun coverageNode(view: ProjectDeclarationView): ProjectOverviewNode? {
        val coverage = (snapshot.occurrenceOf(view.id)?.declaration as? PackageDeclaration)?.callableCoverage
            ?: return null
        if (coverage.complete) return node("callable coverage: complete")
        return node(
            "callable coverage: incomplete",
            children = coverage.omittedNames.map { node("omitted callable: $it") } +
                coverage.gaps.map { node("gap: ${it.name}") }
        )
    }

    private fun memberOwner(view: ProjectDeclarationView): ProjectQualifiedName? = when (view) {
        is ProjectPackageTypeView -> view.owner
        is ProjectSubtypeView -> view.owner
        is ProjectPackageValueView -> view.owner
        is ProjectPackageSubprogramView ->
            if (view.role == ProjectDeclarationRole.SPECIFICATION) view.packageOwner else null
        else -> null
    }

    private fun memberNode(view: ProjectDeclarationView, owner: ProjectQualifiedName): ProjectOverviewNode = when (view) {
        is ProjectPackageTypeView -> shapedTypeNode(view, view.localName.toString(), owner, view.recordFields)
        is ProjectSubtypeView -> node(
            "SUBTYPE ${view.localName} IS ${ref(view.baseType)}${at(view.sourceRange)}",
            range(view.sourceRange),
            children = listOf(typeNode(view.baseType, owner))
        )
        is ProjectPackageValueView -> node(
            "${if (view.isConstant) "CONSTANT" else "VARIABLE"} ${view.localName} ${ref(view.typeRef)}${at(view.sourceRange)}",
            range(view.sourceRange),
            children = listOf(typeNode(view.typeRef, owner))
        )
        is ProjectPackageSubprogramView -> subprogramNode(view, view.localName.toString(), owner)
        else -> error("Not a package member: ${view.kind}")
    }

    private fun shapedTypeNode(
        view: ProjectShapedTypeView,
        name: String,
        owner: ProjectQualifiedName?,
        fields: List<ProjectRecordFieldView>
    ): ProjectOverviewNode {
        val element = view.elementType.orElse(null)
        val shape = view.shape.map { it.name }.orElse("unclassified")
        val fieldNodes = fields.map {
            node(
                "field #${it.ordinal} ${it.name} ${ref(it.typeRef)}",
                range(it.sourceRange),
                children = listOf(typeNode(it.typeRef, owner))
            )
        }
        val elementNode = element?.let {
            node("element ${ref(it)}", range(it.sourceRange), children = listOf(typeNode(it, owner)))
        }
        return node(
            "TYPE $name: $shape${element?.let { " OF ${ref(it)}" }.orEmpty()}${at(view.sourceRange)}",
            range(view.sourceRange),
            children = fieldNodes + listOfNotNull(elementNode)
        )
    }

    private fun subprogramNode(view: ProjectSubprogramView, name: String, owner: ProjectQualifiedName?): ProjectOverviewNode {
        val params = view.parameters.joinToString(", ") { "${it.name} ${mode(it.mode)} ${ref(it.typeRef)}" }
        val function = view as? ProjectFunctionView
        val label = if (function == null) {
            "PROCEDURE $name($params)"
        } else {
            "FUNCTION $name($params) RETURN ${ref(function.returnType)}" +
                if (function.isDeterministic) " DETERMINISTIC" else ""
        }
        val children = mutableListOf<ProjectOverviewNode>()
        view.parameters.mapTo(children) {
            node("parameter ${parameterText(it)}", range(it.sourceRange), children = listOf(typeNode(it.typeRef, owner)))
        }
        if (function != null) {
            val returnType = function.returnType
            children += node(
                "return ${ref(returnType)}",
                range(returnType.sourceRange),
                children = listOf(typeNode(returnType, owner))
            )
        }
        children += if (view.role == ProjectDeclarationRole.BODY) {
            correlationNode(view)
        } else {
            node("written-name discovery (${view.qualifiedName}): " +
                discovery(ProjectCallTarget.written(view.qualifiedName)))
        }
        return node(label + at(view.sourceRange), range(view.sourceRange), children = children)
    }

    private fun correlationNode(view: ProjectDeclarationView): ProjectOverviewNode {
        val correlation = snapshot.correlateSpecification(view.id)
        val specifications = correlation.specification.map { listOf(it) }.orElse(correlation.candidates)
        return node(
            "specification: ${correlation.status}",
            children = specifications.map {
                node("${it.qualifiedName}${at(it.sourceRange)}", range(it.sourceRange))
            }
        )
    }

    private fun parameterText(parameter: ProjectParameterView): String =
        "#${parameter.ordinal} ${parameter.name} ${mode(parameter.mode)}" +
            (if (parameter.isNocopy) " NOCOPY" else "") +
            " ${ref(parameter.typeRef)}" +
            if (parameter.isDefaultPresent) " DEFAULT" else ""

    private fun mode(mode: ProjectParameterMode): String =
        if (mode == ProjectParameterMode.IN_OUT) "IN OUT" else mode.name

    private fun discovery(target: ProjectCallTarget): String {
        val candidates = snapshot.findCallCandidates(target)
        val uncertainties = if (candidates.uncertainties.isEmpty()) {
            ""
        } else {
            " [${candidates.uncertainties.joinToString(", ")}]"
        }
        return "${candidates.status}$uncertainties · ${candidates.candidates.size} candidate(s)"
    }

    private fun typeNode(reference: ProjectTypeReference, owner: ProjectQualifiedName?): ProjectOverviewNode {
        val description = snapshot.describeType(reference, owner)
        val declaration = description.declaration.orElse(null)
        val text = when (description.status) {
            ProjectTypeDescription.Status.DECLARED_TYPE ->
                "declared type ${declaration.qualifiedName} (${declaration.kind})"
            ProjectTypeDescription.Status.BUILT_IN -> "built-in ${description.builtInCategory.get()}"
            ProjectTypeDescription.Status.UNRESOLVED_NAME -> description.resolution.get().let {
                "unresolved name: ${it.status} (${it.candidates.size} candidates)"
            }
            ProjectTypeDescription.Status.ANCHORED_REFERENCE ->
                "anchored reference, stopped: ${description.anchorStop.get()}"
            ProjectTypeDescription.Status.REF_REFERENCE -> "REF reference (not resolved)"
            ProjectTypeDescription.Status.CYCLE -> "cycle"
            ProjectTypeDescription.Status.UNRESOLVED_VALUE ->
                "unresolved value: ${description.valueResolution.get().status}"
        }
        val via = if (description.steps.isEmpty()) {
            ""
        } else {
            " via " + description.steps.joinToString(" → ") { it.qualifiedName.toString() }
        }
        return node("type: $text$via", declaration?.let { range(it.sourceRange) })
    }

    private fun resolutions(): ProjectOverviewNode {
        val typeReferences = mutableListOf<ProjectOverviewNode>()
        val members = mutableListOf<ProjectOverviewNode>()
        val paths = mutableListOf<ProjectOverviewNode>()
        val bodySubprograms = mutableListOf<ProjectOverviewNode>()
        val pending = ArrayDeque<AstNode>().apply { add(root) }
        while (pending.isNotEmpty()) {
            val astNode = pending.removeLast()
            for (index in astNode.children.indices.reversed()) {
                pending.addLast(astNode.children[index])
            }
            val semantic = astNode as? SemanticAstNode ?: continue
            semantic.projectTypeResolution?.let {
                typeReferences += astEntry("${it.reference.name} → ${typeResolutionText(it)}", astNode)
            }
            val path = semantic.projectRecordMemberPathResolution
            val member = semantic.projectRecordMemberResolution
            if (member != null && path == null) {
                members += astEntry("${nodeText(astNode)} → ${memberResolutionText(member)}", astNode)
            }
            if (path != null) {
                val text = when (path) {
                    is ProjectRecordMemberPathResolution.Completed -> "Completed (${path.segments.size} segments)"
                    is ProjectRecordMemberPathResolution.Stopped -> "Stopped before ${path.nextMember}: ${path.reason.name}"
                }
                paths += astEntry("${nodeText(astNode)} → $text", astNode)
            }
            semantic.projectSubprogramDeclaration?.let {
                bodySubprograms += astEntry("${it.kind} ${it.owner}.${it.name} (${it.role})", astNode)
            }
        }
        val typedSymbols = symbolTable.symbols.mapNotNull { symbol ->
            val declaration = symbol.projectTypeDeclaration ?: return@mapNotNull null
            val astNode = symbol.node ?: return@mapNotNull null
            astEntry(
                "${symbol.kind.value} ${symbol.name}: ${declaration.qualifiedName} (${declaration.kind})",
                astNode
            )
        }
        val subgroups = listOfNotNull(
            section("Type references", typeReferences),
            section("RECORD members", members),
            section("RECORD member paths", paths),
            section("Variables typed by project types", typedSymbols),
            section("Package body subprograms linked to facts", bodySubprograms)
        )
        val total = typeReferences.size + members.size + paths.size + typedSymbols.size + bodySubprograms.size
        return node(
            "Resolutions in this file ($total)",
            children = if (total == 0) listOf(node("(none)")) else subgroups
        )
    }

    private fun typeResolutionText(resolution: ProjectTypeResolution): String = when (resolution) {
        is ProjectTypeResolution.Resolved ->
            "Resolved ${resolution.declaration.qualifiedName} (${resolution.declaration.kind})"
        is ProjectTypeResolution.Ambiguous -> "Ambiguous (${resolution.candidates.size} candidates)"
        is ProjectTypeResolution.NotFoundInProject -> "No project declaration"
        is ProjectTypeResolution.IncompleteIndex -> "Incomplete index (${resolution.knownCandidates.size} known)"
        is ProjectTypeResolution.NotPrepared -> "Project index not prepared"
    }

    private fun memberResolutionText(resolution: ProjectRecordMemberResolution): String = when (resolution) {
        is ProjectRecordMemberResolution.Resolved ->
            "field ${resolution.field.name} #${resolution.field.ordinal} of ${resolution.declaration.qualifiedName}"
        is ProjectRecordMemberResolution.NotFound -> "No matching field in ${resolution.declaration.qualifiedName}"
        is ProjectRecordMemberResolution.Ambiguous ->
            "Ambiguous (${resolution.candidates.size} fields) in ${resolution.declaration.qualifiedName}"
        is ProjectRecordMemberResolution.UnsupportedType -> "Unsupported type ${resolution.declaration.qualifiedName}"
    }

    private fun astEntry(label: String, astNode: AstNode) = ProjectOverviewNode(label, nodeRange(astNode), astNode)

    private fun nodeText(astNode: AstNode): String {
        val text = astNode.tokens.joinToString(" ") { it.originalValue }
        return if (text.length > MAX_NODE_TEXT) text.take(MAX_NODE_TEXT) + "…" else text
    }

    private fun nodeRange(astNode: AstNode): SemanticSourceRange? {
        val first = astNode.tokenOrNull ?: return null
        val last = astNode.lastTokenOrNull ?: return null
        return SemanticSourceRange(fileId.value, first.line, first.column, last.endLine, last.endColumn)
    }

    private fun section(title: String, entries: List<ProjectOverviewNode>): ProjectOverviewNode? =
        if (entries.isEmpty()) null else node("$title (${entries.size})", children = entries)

    private fun node(
        label: String,
        range: SemanticSourceRange? = null,
        children: List<ProjectOverviewNode> = emptyList()
    ) = ProjectOverviewNode(label, range, null, children)

    private fun ref(reference: ProjectTypeReference): String = when (reference.kind) {
        ProjectTypeReference.Kind.NAMED -> "${reference.name}"
        ProjectTypeReference.Kind.ANCHORED -> "${reference.name}%${reference.anchor.get()}"
        ProjectTypeReference.Kind.REF -> "REF ${reference.name}"
    }

    private fun range(range: ProjectSourceRange): SemanticSourceRange? =
        if (range.fileId != fileId.value) {
            null
        } else {
            SemanticSourceRange(range.fileId, range.startLine, range.startColumn, range.endLine, range.endColumn)
        }

    private fun at(range: ProjectSourceRange): String =
        if (range.fileId != fileId.value) {
            " @${range.fileId}:${range.startLine}:${range.startColumn}"
        } else {
            " @${range.startLine}:${range.startColumn}"
        }

    private fun contains(outer: ProjectSourceRange, inner: ProjectSourceRange): Boolean =
        compareValues(outer.startLine, inner.startLine).let { it < 0 || it == 0 && outer.startColumn <= inner.startColumn } &&
            compareValues(inner.endLine, outer.endLine).let { it < 0 || it == 0 && inner.endColumn <= outer.endColumn }

    private companion object {
        const val MAX_NODE_TEXT = 60
    }
}
