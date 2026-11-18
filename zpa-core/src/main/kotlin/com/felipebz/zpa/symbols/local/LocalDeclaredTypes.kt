//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription
import com.felipebz.zpa.api.project.ProjectPackageValueView
import com.felipebz.zpa.api.project.ProjectSubtypeView
import com.felipebz.zpa.api.project.ProjectTypeDescription
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.StaticArithmetic
import java.util.*

/** [DeclaredType] answers from the walk [LocalValueCategories.typeCategory] runs, each with the category it gives. */
internal class LocalDeclaredTypes(private val facts: LocalSemanticFacts, private val walk: LocalTypeWalk, private val categories: LocalValueCategories) {
    private val model = facts.model
    private var root: AstNode? = null
    private val datatypes = HashMap<Pair<Int, Int>, AstNode>()
    private val variables = HashMap<Pair<Int, Int>, AstNode>()

    /** A `DATATYPE`, or the name of a local record, collection or REF CURSOR type, as a `DATATYPE` naming it. */
    fun describe(node: AstNode, fields: LocalFields?): DeclaredType {
        if (node.type === PlSqlGrammar.IDENTIFIER_NAME) return declared(node, fields)
        val written = categories.written(node) ?: return unavailable()
        index(node)
        val category = categories.typeCategory(node)
        if (written.external) return unknown(Uncertainty.UNSUPPORTED_REFERENCE, category)
        if (categories.isRefCursorName(written)) return DeclaredType.BuiltIn("SYS_REFCURSOR", WrittenTypeConstraints.NONE, category)
        val evidence = written.evidence
        return when (evidence.terminal) {
            LocalTypeEvidence.Terminal.BUILT_IN -> evidence.terminalUse?.let { builtIn(it, evidence.constrainedUse, category) }
                ?: unknown(Uncertainty.UNSUPPORTED_REFERENCE, category)
            LocalTypeEvidence.Terminal.COMPOSITE -> local(evidence, fields, category)
            LocalTypeEvidence.Terminal.PROJECT -> evidence.description?.let { project(it, evidence.constrainedUse, category) }
                ?: unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
            LocalTypeEvidence.Terminal.LITERAL -> unavailable()
            LocalTypeEvidence.Terminal.UNKNOWN -> evidence.incomplete?.let { DeclaredType.Unknown(categories.reasons(it), category) }
                ?: unknown(Uncertainty.UNSUPPORTED_REFERENCE, category)
        }
    }

    /** The name of a local type declaration, at any depth. */
    private fun declared(name: AstNode, fields: LocalFields?): DeclaredType {
        val type = model.declarations.firstOrNull {
            it.kind in NAMED_TYPES && it.nameRange.startLine == name.token.line && it.nameRange.startColumn == name.token.column
        } ?: return unavailable()
        index(name)
        return typeOf(type, fields, categories.typeCategory(type.region, type))
    }

    private fun builtIn(terminal: LocalTypeUse, constrained: LocalTypeUse?, category: ValueCategory): DeclaredType {
        val syntax = (terminal.typeRef as? NamedTypeRef)?.writtenSyntax ?: return unknown(Uncertainty.UNSUPPORTED_REFERENCE, category)
        return DeclaredType.BuiltIn(syntax.name, constraints(syntax, constrained ?: terminal), category)
    }

    /** The constraints of the predefined type [syntax] as the written type [use] constrains it. */
    private fun constraints(syntax: WrittenTypeSyntax, use: LocalTypeUse): WrittenTypeConstraints {
        val written = (use.typeRef as? NamedTypeRef)?.writtenSyntax
        val node = datatypes[use.range.startLine to use.range.startColumn]
        return syntax.constraints(written) { argument -> node?.let { evaluate(it, argument, use.lookupRegion) } ?: WrittenTypeConstraint.Unknown }
            ?: WrittenTypeConstraints.NONE
    }

    private fun evaluate(datatype: AstNode, argument: ConstraintArgument, region: LocalRegionId): WrittenTypeConstraint {
        val expression = find(datatype, argument.range) ?: return WrittenTypeConstraint.Unknown
        val value = StaticArithmetic.integer(expression) { walk.constantValue(it, region, ::defaultOf, HashSet()) }
        return value?.toIntOrNull()?.let(WrittenTypeConstraint::Value) ?: WrittenTypeConstraint.Unknown
    }

    private fun find(node: AstNode, range: SourceRange): AstNode? {
        if (LocalSyntax.range(model.fileId, node) == range) return node
        return node.children.firstNotNullOfOrNull { find(it, range) }
    }

    private fun defaultOf(constant: LocalDeclaration): AstNode? =
        variables[constant.range.startLine to constant.range.startColumn]?.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)?.lastChild

    private fun local(evidence: LocalTypeEvidence, fields: LocalFields?, category: ValueCategory): DeclaredType {
        // A package cursor's query row: the `%ROWTYPE` as written, as the project describes it.
        if (evidence.selectList != null) {
            return evidence.description?.let { project(it, evidence.constrainedUse, category) } ?: unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
        }
        val last = evidence.followed.lastOrNull()?.let(model::declaration) ?: return unknown(Uncertainty.UNSUPPORTED_REFERENCE, category)
        return typeOf(last, fields, category)
    }

    private fun typeOf(last: LocalDeclaration, fields: LocalFields?, category: ValueCategory): DeclaredType {
        val declaration = facts.names.declaration(last.id)
        return when (last.kind) {
            LocalDeclarationKind.RECORD_TYPE -> DeclaredType.Record(declaration, fields?.recordFields(last.id).orEmpty(), category)
            LocalDeclarationKind.COLLECTION_TYPE ->
                DeclaredType.Collection(declaration, collectionKind(last), last.type?.range?.let { datatypes[it.startLine to it.startColumn] }, category)
            else -> DeclaredType.OtherLocal(declaration, category)
        }
    }

    private fun project(description: ProjectExternalTypeDescription, constrained: LocalTypeUse?, category: ValueCategory): DeclaredType {
        // An anchor the project leaves to external metadata, or one an external reading competes with.
        val anchor = description.anchor.orElse(null)
        if (anchor != null && description.status != ProjectExternalTypeDescription.Status.NOT_EXTERNAL) {
            return DeclaredType.Anchored(anchor, description.anchorName.get(), description, category)
        }
        val source = description.source
        return when (source.status) {
            ProjectTypeDescription.Status.BUILT_IN -> source.terminalReference.orElse(null)?.writtenSyntax?.let { syntax ->
                DeclaredType.BuiltIn(syntax.name, projectConstraints(syntax, source, constrained), category)
            } ?: unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
            ProjectTypeDescription.Status.DECLARED_TYPE -> source.declaration.orElse(null)?.let { DeclaredType.Project(it, description, category) }
                ?: unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
            ProjectTypeDescription.Status.ANCHORED_REFERENCE -> source.writtenAnchor.orElse(null)?.let { DeclaredType.Anchored(it.anchor, it.name, description, category) }
                ?: unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
            else -> unknown(Uncertainty.DECLARATION_OUTSIDE_FILE, category)
        }
    }

    /**
     * A local use written with constraints applies first; else the nearest step of the project's walk (a package value's
     * written type, a subtype's base type) that writes some, whose constants the index does not value; else the
     * predefined type's own.
     */
    private fun projectConstraints(syntax: WrittenTypeSyntax, source: ProjectTypeDescription, constrained: LocalTypeUse?): WrittenTypeConstraints {
        if (constrained != null) return constraints(syntax, constrained)
        val written = source.steps.firstNotNullOfOrNull { step ->
            val reference = when (step) {
                is ProjectPackageValueView -> step.typeRef
                is ProjectSubtypeView -> step.baseType
                else -> null
            }
            reference?.writtenSyntax?.takeIf { it.groups.isNotEmpty() }
        } ?: syntax
        return syntax.constraints(written) { WrittenTypeConstraint.Unknown } ?: WrittenTypeConstraints.NONE
    }

    private fun index(node: AstNode) {
        val top = generateSequence(node) { it.parentOrNull }.last()
        if (top === root) return
        root = top
        datatypes.clear()
        variables.clear()
        fun visit(current: AstNode) {
            when (current.type) {
                PlSqlGrammar.DATATYPE -> datatypes[current.token.line to current.token.column] = current
                PlSqlGrammar.VARIABLE_DECLARATION -> variables[current.token.line to current.token.column] = current
            }
            current.children.forEach(::visit)
        }
        visit(top)
    }

    private fun unknown(reason: Uncertainty, category: ValueCategory) = DeclaredType.Unknown(EnumSet.of(reason), category)

    private fun unavailable() = DeclaredType.Unavailable(Unavailability.NOT_A_COVERED_NAME)

    private companion object {
        val NAMED_TYPES = EnumSet.of(LocalDeclarationKind.RECORD_TYPE, LocalDeclarationKind.COLLECTION_TYPE, LocalDeclarationKind.REF_CURSOR_TYPE)
    }
}
