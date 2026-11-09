//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectArgumentType
import com.felipebz.zpa.project.SourceRange

/**
 * The local semantic resolution of one file, computed by the pipeline's pre-pass: the model, the references with their
 * segment-by-segment outcomes, and the calls. AST-detached.
 */
internal class LocalSemanticFacts(
    val model: LocalSemanticModel,
    val references: List<LocalReference>,
    /**
     * For each reference, in the same order, its outcomes as [LocalResolver.resolveQualified] gives them, refined with
     * the project's specification coverage ([LocalSpecificationCoverage]) when the project is prepared.
     */
    val resolutions: List<List<LocalResolution>>,
    val calls: List<LocalCallAnalysis>,
    /**
     * For each variable or exception name declared inside a package body whose specification is in another file, keyed
     * by the body region and the name's lookup form: the specification's variable or exception of that name, or
     * [LocalSpecificationValue.Unknown]. Absent when the project is not prepared.
     */
    val specificationValues: Map<Pair<LocalRegionId, String>, LocalSpecificationValue> = emptyMap(),
    /** See [LocalSpecificationCoverage.sqlSpecificationMembers]. */
    val sqlSpecificationMembers: Map<Pair<Int, Int>, LocalSpecificationMember> = emptyMap(),
    /** The names written as members of a value (after a call, an element access or a parenthesized expression). */
    val members: List<LocalMember> = emptyList(),
    /** See [LocalForwardCalls.itemActualTypes]. */
    val itemActualTypes: Map<Pair<Int, Int>, List<ProjectArgumentType>> = emptyMap()
) {
    /** The usage evidence of the file's declarations, computed when first asked. */
    val usages: LocalUsages by lazy { LocalUsages(this) }

    /** Which declaration each of the file's declarations hides, computed when first asked. */
    val hiding: LocalHiding by lazy { LocalHiding(this) }

    /** The exceptions named in `RAISE` statements and handlers, computed when first asked. */
    val exceptions: LocalExceptions by lazy { LocalExceptions(this) }

    /** What the plain names in expressions bind, computed when first asked. */
    val names: LocalNames by lazy { LocalNames(this) }

    /** The local resolution of the call whose name starts at line, column; null when the local side has none. */
    fun localCall(line: Int, column: Int): LocalCallResolution? =
        calls.singleOrNull { it.reference.range.startLine == line && it.reference.range.startColumn == column }?.local

    /**
     * The declaration [outcome] names and whether that is certain: a binding or an erroneous use of one declaration is
     * certain; a provisional outcome counts only with exactly one candidate. Null for any other outcome. With several
     * overloads, the one the call resolution selects when [whole] is the name of the call.
     */
    fun declarationOf(reference: LocalReference, outcome: LocalResolution, whole: Boolean): Pair<LocalDeclarationId, Boolean>? =
        when (outcome) {
            is LocalResolution.Bound -> outcome.declaration to true
            is LocalResolution.WrongKind -> outcome.declaration to true
            is LocalResolution.DeclaredLater -> outcome.declaration to true
            is LocalResolution.OutOfScope -> outcome.declaration to true
            is LocalResolution.InstanceMemberInStatic -> outcome.declaration to true
            is LocalResolution.OverloadSet -> overload(reference, outcome.declarations, whole)?.let { it to true }
            is LocalResolution.LocalFunctionInSql -> overload(reference, outcome.declarations, whole)?.let { it to true }
            is LocalResolution.SqlColumnOrLocal -> outcome.declaration to false
            is LocalResolution.SqlBuiltinOrLocal -> outcome.declarations.singleOrNull()?.let { it to false }
            is LocalResolution.SpecificationCovered -> declarationOf(reference, outcome.outcome, whole)
            is LocalResolution.Incomplete -> declarationOf(reference, outcome.provisional, whole)?.let { it.first to false }
            is LocalResolution.Unsupported -> outcome.candidate?.let { it to false }
            else -> null
        }

    private fun overload(reference: LocalReference, declarations: List<LocalDeclarationId>, whole: Boolean): LocalDeclarationId? {
        declarations.singleOrNull()?.let { return it }
        if (!whole) return null
        val call = localCall(reference.range.startLine, reference.range.startColumn) ?: return null
        return call.selected.takeIf { call.status == LocalCallResolution.Status.RESOLVED }
    }

    /** The single non-type reference starting at line, column, with its outcomes; null when none or several start there. */
    private fun referenceAt(line: Int, column: Int): Int? = byStart[Triple(false, line, column)]?.singleOrNull()

    /**
     * The declaration the first segment of the name starting at line, column names, by [declarationOf]; null when no
     * single reference starts there or its outcome names no one declaration.
     */
    fun nameDeclaration(line: Int, column: Int): LocalDeclaration? {
        val index = referenceAt(line, column) ?: return null
        val reference = references[index]
        return declarationOf(reference, resolutions[index].first(), reference.segments.isEmpty())?.first?.let(model::declaration)
    }

    /**
     * The declaration the whole qualified name starting at line, column binds as a local name (`proc.v`, `label.v`,
     * `pkg.v` with the package in this file), by [declarationOf] on each segment: every segment before the last names a
     * label, subprogram or package. Null when it is not one: a member of a value (`v.f`, `self.a`), an
     * attribute, or a segment naming no one declaration.
     */
    fun qualifiedDeclaration(line: Int, column: Int): LocalDeclaration? {
        val index = referenceAt(line, column) ?: return null
        val reference = references[index]
        val steps = resolutions[index]
        if (reference.segments.isEmpty() || steps.size != reference.segments.size + 1 || reference.segments.any { it.isAttribute }) return null
        for (step in 0 until steps.size - 1) {
            val qualifier = declarationOf(reference, steps[step], false)?.first?.let(model::declaration) ?: return null
            if (qualifier.kind !in QUALIFIER_KINDS) return null
        }
        return declarationOf(reference, steps.last(), true)?.first?.let(model::declaration)
    }

    /** Every step of the name starting at line, column, when a single non-type reference starts there. */
    fun steps(line: Int, column: Int): List<LocalResolution>? =
        byStart[Triple(false, line, column)]?.singleOrNull()?.let { resolutions[it] }

    private val byStart: Map<Triple<Boolean, Int, Int>, List<Int>> by lazy {
        val map = HashMap<Triple<Boolean, Int, Int>, MutableList<Int>>()
        for ((index, reference) in references.withIndex()) {
            map.getOrPut(Triple(isTypeName(reference), reference.range.startLine, reference.range.startColumn)) { ArrayList(1) } += index
        }
        map
    }

}

/** A source range as plain coordinates, detached from any file object. */
internal data class LocalRangeKey(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int) {
    companion object {
        fun of(range: SourceRange) = LocalRangeKey(range.startLine, range.startColumn, range.endLine, range.endColumn)
    }
}


/** The declarations that qualify a local name in a qualified reference. */
private val QUALIFIER_KINDS = setOf(
    LocalDeclarationKind.LABEL, LocalDeclarationKind.PROCEDURE, LocalDeclarationKind.FUNCTION,
    LocalDeclarationKind.PACKAGE, LocalDeclarationKind.PACKAGE_BODY
)

/** A written type name, one segment or qualified (whose first segment is a qualifier of a type position). */
internal fun isTypeName(reference: LocalReference) = reference.lastSegmentContext == LocalReferenceContext.TYPE
