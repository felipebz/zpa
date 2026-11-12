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

/** [DeclaredType] answers from the walk [LocalValueCategories.typeCategory] runs. */
internal class LocalDeclaredTypes(private val facts: LocalSemanticFacts, private val walk: LocalTypeWalk, private val categories: LocalValueCategories) {
    private val model = facts.model
    private var root: AstNode? = null
    private val datatypes = HashMap<Pair<Int, Int>, AstNode>()
    private val variables = HashMap<Pair<Int, Int>, AstNode>()

    fun describe(datatype: AstNode, fields: LocalFields?): DeclaredType {
        val written = categories.written(datatype) ?: return unavailable()
        index(datatype)
        if (written.external) return unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        if (categories.isRefCursorName(written)) return DeclaredType.BuiltIn("SYS_REFCURSOR", WrittenTypeConstraints.NONE)
        val evidence = written.evidence
        return when (evidence.terminal) {
            LocalTypeEvidence.Terminal.BUILT_IN -> evidence.terminalUse?.let { builtIn(it, evidence.constrainedUse) } ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
            LocalTypeEvidence.Terminal.COMPOSITE -> local(evidence, fields)
            LocalTypeEvidence.Terminal.PROJECT -> evidence.description?.let { project(it, evidence.constrainedUse) } ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            LocalTypeEvidence.Terminal.LITERAL -> unavailable()
            LocalTypeEvidence.Terminal.UNKNOWN -> evidence.incomplete?.let { DeclaredType.Unknown(categories.reasons(it)) }
                ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        }
    }

    private fun builtIn(terminal: LocalTypeUse, constrained: LocalTypeUse?): DeclaredType {
        val syntax = (terminal.typeRef as? NamedTypeRef)?.writtenSyntax ?: return unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        return DeclaredType.BuiltIn(syntax.name, constraints(syntax, constrained ?: terminal))
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

    private fun local(evidence: LocalTypeEvidence, fields: LocalFields?): DeclaredType {
        val last = evidence.followed.lastOrNull()?.let(model::declaration) ?: return unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        val declaration = facts.names.declaration(last.id)
        return when (last.kind) {
            LocalDeclarationKind.RECORD_TYPE -> DeclaredType.Record(declaration, fields?.recordFields(last.id).orEmpty())
            LocalDeclarationKind.COLLECTION_TYPE ->
                DeclaredType.Collection(declaration, collectionKind(last), last.type?.range?.let { datatypes[it.startLine to it.startColumn] })
            else -> DeclaredType.OtherLocal(declaration)
        }
    }

    private fun project(description: ProjectExternalTypeDescription, constrained: LocalTypeUse?): DeclaredType {
        // An anchor the project leaves to external metadata, or one an external reading competes with.
        val anchor = description.anchor.orElse(null)
        if (anchor != null && description.status != ProjectExternalTypeDescription.Status.NOT_EXTERNAL) {
            return DeclaredType.Anchored(anchor, description.anchorName.get(), description)
        }
        val source = description.source
        return when (source.status) {
            ProjectTypeDescription.Status.BUILT_IN -> source.terminalReference.orElse(null)?.writtenSyntax?.let { syntax ->
                DeclaredType.BuiltIn(syntax.name, projectConstraints(syntax, source, constrained))
            } ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            ProjectTypeDescription.Status.DECLARED_TYPE -> source.declaration.orElse(null)?.let { DeclaredType.Project(it, description) }
                ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            ProjectTypeDescription.Status.ANCHORED_REFERENCE -> source.writtenAnchor.orElse(null)?.let { DeclaredType.Anchored(it.anchor, it.name, description) }
                ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            else -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
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

    private fun unknown(reason: UsageUncertainty) = DeclaredType.Unknown(EnumSet.of(reason))

    private fun unavailable() = DeclaredType.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)
}
