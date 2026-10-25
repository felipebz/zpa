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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.*
import java.util.*

/**
 * The exceptions that names in `RAISE` statements and `WHEN` handlers bind, from the file's reference outcomes, and the
 * completeness of the regions that hold nodes.
 */
internal class LocalExceptions(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val referencesByStart = facts.references.indices
        .filter { facts.references[it].lastSegmentContext == LocalReferenceContext.EXCEPTION }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    /** The code each declaration's `PRAGMA EXCEPTION_INIT` gives it (null when not a literal). */
    private var codes: Map<LocalDeclarationId, Int?>? = null

    /** The region and name of each pragma the resolution did not decide, with what it could not see. */
    private var undecidedPragmas: Map<Pair<LocalRegionId, String>, Set<UsageUncertainty>> = emptyMap()

    fun binding(name: AstNode): ExceptionBinding {
        if (name.type != PlSqlGrammar.VARIABLE_NAME && name.type != PlSqlGrammar.MEMBER_EXPRESSION) return unavailable()
        val parent = name.parentOrNull?.type
        if (parent != PlSqlGrammar.RAISE_STATEMENT && parent != PlSqlGrammar.EXCEPTION_HANDLER) return unavailable()
        val index = referencesByStart[name.token.line to name.token.column] ?: return unavailable()
        if (codes == null) collectPragmas(name)
        val reference = facts.references[index]
        val steps = facts.resolutions[index]
        val outcome = steps.last()
        return when {
            outcome is LocalResolution.Incomplete -> ExceptionBinding.Unknown(reasons(outcome))
            outcome is LocalResolution.Unsupported -> ExceptionBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
            // A qualified name not followed to its last segment: a qualifier the file does not declare names a schema object.
            steps.size != reference.segments.size + 1 ->
                if (unwrap(outcome) == LocalResolution.ProvenAbsent) ExceptionBinding.NotLocal
                else ExceptionBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
            else -> decided(reference, unwrap(outcome))
        }
    }

    private fun unwrap(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    private fun decided(reference: LocalReference, outcome: LocalResolution): ExceptionBinding = when (outcome) {
        LocalResolution.SpecificationMember -> specificationMember(reference)
        is LocalResolution.Bound -> model.declaration(outcome.declaration).let { declaration ->
            if (declaration.kind == LocalDeclarationKind.EXCEPTION) ExceptionBinding.Declared(local(declaration)) else ExceptionBinding.Invalid
        }
        LocalResolution.ProvenAbsent ->
            if (reference.segments.isEmpty() && reference.name.lookupName in PREDEFINED) ExceptionBinding.Predefined(reference.name.lookupName)
            else ExceptionBinding.NotLocal
        LocalResolution.Undeclared -> ExceptionBinding.NotLocal
        else -> ExceptionBinding.Invalid
    }

    /** A member of the enclosing package's specification in another file, named alone or through the package. */
    private fun specificationMember(reference: LocalReference): ExceptionBinding {
        val body = LocalResolver.chain(model, reference.region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_BODY }
        val unit = body?.let { LocalTypeScope.unitOf(model, it.id) }?.unitName ?: return ExceptionBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        val name = (reference.segments.lastOrNull()?.name ?: reference.name).lookupName
        return ExceptionBinding.Declared(ExceptionDeclaration(
            "specification:${unit.lookupKey()}.$name", ExceptionScope.PACKAGE_SPECIFICATION, null, null,
            ExceptionErrorCode.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        ))
    }

    private fun local(declaration: LocalDeclaration): ExceptionDeclaration {
        val scope = when (model.region(declaration.region).kind) {
            LocalRegionKind.PACKAGE_SPEC -> ExceptionScope.PACKAGE_SPECIFICATION
            LocalRegionKind.PACKAGE_BODY -> ExceptionScope.PACKAGE_BODY
            LocalRegionKind.SUBPROGRAM -> ExceptionScope.SUBPROGRAM
            LocalRegionKind.TRIGGER, LocalRegionKind.TIMING_POINT -> ExceptionScope.TRIGGER
            else -> ExceptionScope.BLOCK
        }
        return ExceptionDeclaration(
            "local:${declaration.id.ordinal}", scope, declaration.nameRange.startLine, declaration.nameRange.startColumn, errorCode(declaration)
        )
    }

    /** The pragma must be in the exception's declarative part: a gap in that region may hide it. */
    private fun errorCode(declaration: LocalDeclaration): ExceptionErrorCode {
        val codes = codes.orEmpty()
        val hidden = model.region(declaration.region).completeness.reasons
        return when {
            hidden.isNotEmpty() -> ExceptionErrorCode.Unknown(hidden.mapTo(EnumSet.noneOf(UsageUncertainty::class.java)) { uncertainty(it) })
            declaration.id in codes -> ExceptionErrorCode.Associated(codes[declaration.id])
            else -> undecidedPragmas[declaration.region to declaration.name.lookupName]?.let { ExceptionErrorCode.Unknown(it) } ?: ExceptionErrorCode.None
        }
    }

    private fun collectPragmas(node: AstNode) {
        var root = node
        while (true) root = root.parentOrNull ?: break
        val codes = HashMap<LocalDeclarationId, Int?>()
        val undecided = HashMap<Pair<LocalRegionId, String>, MutableSet<UsageUncertainty>>()
        for (pragma in root.getDescendants(PlSqlGrammar.EXCEPTION_INIT_PRAGMA)) {
            val name = pragma.getFirstChildOrNull(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) ?: continue
            val index = referencesByStart[name.token.line to name.token.column] ?: continue
            val reference = facts.references[index]
            val outcome = unwrap(facts.resolutions[index].last())
            val declaration = (outcome as? LocalResolution.Bound)?.let { model.declaration(it.declaration) }
            if (declaration != null) {
                val code = pragma.getFirstChildOrNull(PlSqlPunctuator.COMMA)?.nextSibling
                codes[declaration.id] = code?.tokens?.joinToString("") { it.value }?.toIntOrNull()
            } else if (outcome is LocalResolution.Incomplete || outcome is LocalResolution.Unsupported) {
                val reasons = (outcome as? LocalResolution.Incomplete)?.let { reasons(it) } ?: EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE)
                val key = reference.region to (reference.segments.lastOrNull()?.name ?: reference.name).lookupName
                undecided.getOrPut(key) { EnumSet.noneOf(UsageUncertainty::class.java) } += reasons
            }
        }
        this.codes = codes
        undecidedPragmas = undecided
    }

    /**
     * What the analysis cannot see in the region that holds [node]: for a block or subprogram, the region of its `BEGIN`.
     * The regions inside it count too.
     */
    fun regionUncertainty(node: AstNode): Set<UsageUncertainty> {
        val token = (node.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION) ?: node).token
        val region = model.regions.filter { contains(it, token.line, token.column) }.maxByOrNull { depth(it) } ?: return emptySet()
        val reasons = model.regions.filter { within(it, region) }.flatMapTo(EnumSet.noneOf(LocalIncompleteness::class.java)) { it.completeness.reasons }
        return reasons.mapTo(EnumSet.noneOf(UsageUncertainty::class.java)) { uncertainty(it) }
    }

    private fun uncertainty(reason: LocalIncompleteness) = when (reason) {
        LocalIncompleteness.CONDITIONAL_COMPILATION -> UsageUncertainty.CONDITIONAL_COMPILATION
        LocalIncompleteness.SUBSTITUTION -> UsageUncertainty.SUBSTITUTION
        LocalIncompleteness.PARSE_RECOVERY -> UsageUncertainty.PARSE_RECOVERY
        else -> UsageUncertainty.INCOMPLETE_SOURCE
    }

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach { reasons += uncertainty(it) }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private fun contains(region: LocalRegion, line: Int, column: Int): Boolean {
        val range = region.range
        return (range.startLine < line || range.startLine == line && range.startColumn <= column) &&
            (line < range.endLine || line == range.endLine && column < range.endColumn)
    }

    /** Whether [region] is [outer] or a region inside it. */
    private fun within(region: LocalRegion, outer: LocalRegion): Boolean {
        var current: LocalRegion? = region
        while (current != null) {
            if (current.id == outer.id) return true
            current = model.parent(current.id)
        }
        return false
    }

    private fun depth(region: LocalRegion): Int {
        var depth = 0
        var current = model.parent(region.id)
        while (current != null) { depth++; current = model.parent(current.id) }
        return depth
    }

    private fun unavailable() = ExceptionBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)

    private companion object {
        /** The exceptions package STANDARD declares. */
        val PREDEFINED = setOf(
            "ACCESS_INTO_NULL", "CASE_NOT_FOUND", "COLLECTION_IS_NULL", "CURSOR_ALREADY_OPEN", "DUP_VAL_ON_INDEX",
            "INVALID_CURSOR", "INVALID_NUMBER", "LOGIN_DENIED", "NO_DATA_FOUND", "NO_DATA_NEEDED", "NOT_LOGGED_ON",
            "PROGRAM_ERROR", "ROWTYPE_MISMATCH", "SELF_IS_NULL", "STORAGE_ERROR", "SUBSCRIPT_BEYOND_COUNT",
            "SUBSCRIPT_OUTSIDE_LIMIT", "SYS_INVALID_ROWID", "TIMEOUT_ON_RESOURCE", "TOO_MANY_ROWS", "VALUE_ERROR",
            "ZERO_DIVIDE"
        )
    }
}
