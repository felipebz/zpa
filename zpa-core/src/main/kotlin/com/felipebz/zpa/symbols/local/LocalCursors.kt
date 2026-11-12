//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectPackageCursorView
import com.felipebz.zpa.api.project.ProjectValueResolution
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ParameterListBinding
import com.felipebz.zpa.project.ProjectCallArgument
import com.felipebz.zpa.project.ProjectCallArguments
import com.felipebz.zpa.project.ProjectCallCandidate
import com.felipebz.zpa.project.ProjectCallKind
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/**
 * The cursor each cursor position of the file names (`OPEN`, `FETCH`, `CLOSE`, a cursor `FOR` loop, a `%ROWTYPE`
 * anchor), from the file's reference outcomes and the project the walk holds, with the actuals of `OPEN` and of the loop
 * bound to the cursor's parameters by the binder calls use.
 */
internal class LocalCursors(private val facts: LocalSemanticFacts, private val walk: LocalTypeWalk) {
    private val model = facts.model
    private val referencesByStart = referenceStarts(facts.references) { it.lastSegmentContext in CONTEXTS }

    fun binding(name: AstNode): CursorBinding {
        val written = writtenNames(name) ?: return NOT_A_CURSOR
        val actuals = invocation(name) ?: return NOT_A_CURSOR
        val index = referencesByStart[name.token.line, name.token.column]
        if (index < 0) return NOT_A_CURSOR
        val reference = facts.references[index]
        if (reference.segments.size != written - 1 || reference.segments.any { it.isAttribute }) return NOT_A_CURSOR
        return resolve(reference, facts.resolutions[index], actuals.orElse(null))
    }

    /** The number of names [name] writes (`pk.c` writes two); null for anything but a plain or dotted name. */
    private fun writtenNames(name: AstNode): Int? = when (name.type) {
        PlSqlGrammar.VARIABLE_NAME -> 1
        PlSqlGrammar.MEMBER_EXPRESSION -> name.takeIf { it.firstChild.type === PlSqlGrammar.VARIABLE_NAME }?.children?.drop(1)
            ?.takeIf { rest -> rest.all { it.type === PlSqlPunctuator.DOT || it.type === PlSqlGrammar.IDENTIFIER_NAME } }
            ?.count { it.type === PlSqlGrammar.IDENTIFIER_NAME }?.plus(1)
        else -> null
    }

    /**
     * The labels of the actuals [name] is invoked with (null for a positional one): those of `OPEN` and of a cursor `FOR`
     * loop, none when no list is written; empty for `FETCH`, `CLOSE` and `%ROWTYPE`, which invoke nothing. Null when
     * [name] is in no such position.
     */
    private fun invocation(name: AstNode): Optional<List<OracleIdentifier?>>? {
        val parent = name.parentOrNull ?: return null
        return when {
            parent.type === PlSqlGrammar.OPEN_STATEMENT && parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) === name ->
                Optional.of(labels(parent.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)))
            (parent.type === PlSqlGrammar.FETCH_STATEMENT || parent.type === PlSqlGrammar.CLOSE_STATEMENT) &&
                parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) === name -> Optional.empty()
            parent.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL -> Optional.of(emptyList())
            parent.type === PlSqlGrammar.METHOD_CALL && parent.firstChild === name && parent.parentOrNull?.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL ->
                parent.getChildren(PlSqlGrammar.ARGUMENTS).singleOrNull()?.let { Optional.of(labels(it)) }
            parent.type === PlSqlGrammar.ANCHORED_DATATYPE && parent.firstChild === name && parent.lastChild.type === PlSqlKeyword.ROWTYPE -> Optional.empty()
            else -> null
        }
    }

    private fun labels(arguments: AstNode?): List<OracleIdentifier?> = arguments?.getChildren(PlSqlGrammar.ARGUMENT).orEmpty().map { argument ->
        if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) LocalSyntax.identifier(argument.firstChild) else null
    }

    private fun resolve(reference: LocalReference, steps: List<LocalResolution>, actuals: List<OracleIdentifier?>?): CursorBinding {
        val name = QualifiedName(listOf(reference.name) + reference.segments.map { it.name })
        if (reference.segments.isNotEmpty() && LocalReferenceOutcomes.isAbsent(steps.first())) return project(name, reference.region, actuals)
        val member = steps.indexOfFirst(LocalReferenceOutcomes::isSpecificationMember)
        if (member >= 0) {
            // A cursor of the package's specification in another file, named alone or through the package.
            val full = (if (member == 0) walk.specificationMember(name.segments, reference.region) else name) ?: return OUTSIDE_FILE
            return project(full, reference.region, actuals)
        }
        steps.firstOrNull { covered(it) is LocalResolution.Incomplete }?.let { return CursorBinding.Unknown(reasons(covered(it) as LocalResolution.Incomplete)) }
        val last = covered(steps.last()) as? LocalResolution.Bound ?: return NOT_A_CURSOR
        val cursor = model.declaration(last.declaration).takeIf { steps.size == reference.segments.size + 1 && it.kind == LocalDeclarationKind.CURSOR }
            ?: return NOT_A_CURSOR
        return CursorBinding.Local(facts.names.declaration(cursor.id), actuals?.let { bindLocal(cursor, it) })
    }

    private fun project(name: QualifiedName, region: LocalRegionId, actuals: List<OracleIdentifier?>?): CursorBinding {
        walk.projectCursor(name, region)?.let { cursor -> return CursorBinding.Project(cursor, actuals?.let { bindProject(cursor, it) }) }
        // A package variable of that name (a cursor variable): no explicit cursor.
        val member = walk.packageMember(name, region)
        return if (member?.status == ProjectValueResolution.Status.RESOLVED) NOT_A_CURSOR else OUTSIDE_FILE
    }

    private fun bindLocal(cursor: LocalDeclaration, actuals: List<OracleIdentifier?>): CursorArguments {
        val parameters = (cursor.details as? LocalCursor)?.parameters?.let { region ->
            model.region(region).declarations.map(model::declaration).filter { it.kind == LocalDeclarationKind.CURSOR_PARAMETER }
                .sortedBy { (it.details as LocalCursorParameter).ordinal }
        }.orEmpty()
        return bind(parameters, LocalDeclaration::name, LocalDeclaration::hasDefault, actuals) {
            CursorParameter(it.name.originalSpelling, (it.details as LocalCursorParameter).ordinal, it.hasDefault, it.nameRange.startLine, it.nameRange.startColumn)
        }
    }

    private fun bindProject(cursor: ProjectPackageCursorView, actuals: List<OracleIdentifier?>): CursorArguments =
        bind(cursor.parameters, { it.name.identifier }, { it.isDefaultPresent }, actuals) {
            val range = it.sourceRange
            CursorParameter(it.name.originalSpelling, it.ordinal, it.isDefaultPresent, range.startLine, range.startColumn, range.fileId.takeUnless { id -> id == model.fileId.value })
        }

    private fun <P> bind(
        parameters: List<P>,
        name: (P) -> OracleIdentifier,
        hasDefault: (P) -> Boolean,
        actuals: List<OracleIdentifier?>,
        parameter: (P) -> CursorParameter
    ): CursorArguments {
        val arguments = ProjectCallArguments.of(actuals.map { label -> label?.let(ProjectCallArgument::named) ?: ProjectCallArgument.positional() })
        if (arguments.notation != ProjectCallArguments.Notation.VALID) {
            return CursorArguments(CursorArguments.Status.INVALID_NOTATION, emptyList(), emptyList(), emptyList(), arguments.invalidArgumentOrdinal)
        }
        // A cursor is invoked as it is declared: the kind never differs.
        val result = ParameterListBinding.bind(ProjectCallKind.FUNCTION, ProjectCallKind.FUNCTION, parameters, name, hasDefault, arguments)
        val status = when (result.applicability) {
            ProjectCallCandidate.Applicability.APPLICABLE -> CursorArguments.Status.BOUND
            ProjectCallCandidate.Applicability.TOO_MANY_ARGUMENTS -> CursorArguments.Status.TOO_MANY_ARGUMENTS
            ProjectCallCandidate.Applicability.UNKNOWN_LABEL -> CursorArguments.Status.UNKNOWN_LABEL
            ProjectCallCandidate.Applicability.DUPLICATE_BINDING -> CursorArguments.Status.DUPLICATE_BINDING
            ProjectCallCandidate.Applicability.MISSING_REQUIRED -> CursorArguments.Status.MISSING_REQUIRED
            ProjectCallCandidate.Applicability.INVALID_SIGNATURE, ProjectCallCandidate.Applicability.WRONG_KIND -> CursorArguments.Status.INVALID_SIGNATURE
        }
        val bindings = if (status != CursorArguments.Status.BOUND) emptyList()
        else result.formalIndices.mapIndexed { index, formal -> CursorArgumentBinding(index + 1, parameter(parameters[formal])) }
        return CursorArguments(status, bindings, result.omitted.map(parameter), result.missing.map(parameter), result.rejectedArgumentOrdinal)
    }

    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach { reasons += it.usageUncertainty() }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private companion object {
        val CONTEXTS: Set<LocalReferenceContext> =
            EnumSet.of(LocalReferenceContext.CURSOR_OPERAND, LocalReferenceContext.FOR_CONTROL, LocalReferenceContext.ANCHOR_ROWTYPE)
        val NOT_A_CURSOR = CursorBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)
        val OUTSIDE_FILE = CursorBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
    }
}
