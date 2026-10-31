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
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.*
import java.util.*

/**
 * What the plain names and call names written in expressions bind, and the segments of qualified names, from the
 * file's reference outcomes.
 */
internal class LocalNames(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val referencesByStart = facts.references.indices
        .filter { facts.references[it].segments.isEmpty() && facts.references[it].context in CONTEXTS }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }
    private val qualifiedByStart = facts.references.indices
        .filter { facts.references[it].segments.isNotEmpty() && facts.references[it].lastSegmentContext in CONTEXTS }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    /** The qualified names whose segments are answered one by one: those of [CONTEXTS], and method call statements. */
    private val chainIndices = facts.references.indices
        .filter { facts.references[it].segments.isNotEmpty() && facts.references[it].lastSegmentContext in CHAIN_CONTEXTS }
    private val chainsByStart = chainIndices.associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    /** The segments after the first, by the start of their name: the reference and the segment's position in it (1 for the second). */
    private val segmentsByStart = chainIndices.flatMap { index ->
        facts.references[index].segments.mapIndexed { position, segment -> (segment.range.startLine to segment.range.startColumn) to (index to position + 1) }
    }.toMap()

    fun binding(name: AstNode): NameBinding = when (name.type) {
        PlSqlGrammar.MEMBER_EXPRESSION -> qualified(name)
        PlSqlGrammar.VARIABLE_NAME -> {
            val start = name.token.line to name.token.column
            val index = referencesByStart[start]
            when {
                index != null -> first(index, facts.resolutions[index].last())
                else -> chainsByStart[start]?.let { first(it, facts.resolutions[it].first()) } ?: unavailable()
            }
        }
        PlSqlGrammar.IDENTIFIER_NAME -> segmentsByStart[name.token.line to name.token.column]?.let { (index, position) -> segment(index, position) }
            ?: unavailable()
        else -> unavailable()
    }

    /** A plain name, or the first name of a qualified one: [outcome] is what the reference's own name resolved to. */
    private fun first(index: Int, outcome: LocalResolution): NameBinding {
        val reference = facts.references[index]
        // Over DUAL alone no column but DUMMY exists: the SQL reading is decided.
        val noColumn = reference.sql?.excludesColumn(reference.name) == true
        return when (val decided = unwrap(outcome)) {
            is LocalResolution.Incomplete -> specificationMember(reference, decided, noColumn) ?: incomplete(decided, noColumn)
            is LocalResolution.SqlColumnOrLocal ->
                if (noColumn) NameBinding.Local(declaration(decided.declaration)) else NameBinding.ColumnOrLocal(declaration(decided.declaration))
            // A table, view or alias of the statement takes the first name of `x.f`.
            LocalResolution.SqlRelation -> NameBinding.NothingLocal(sqlColumnPossible = true)
            else -> decided(decided)
        }
    }

    /**
     * The segment at [position] of a qualified name: the declaration it binds when every segment before it names a unit
     * or a block (`pkg.v`, `pkg.g_rec.f`, `fn.p`). A segment after a value (a record field, a collection method, an
     * object member) or an attribute after `%` is not such a name, nor is the procedure a call statement names.
     */
    private fun segment(index: Int, position: Int): NameBinding {
        val reference = facts.references[index]
        val segments = reference.segments
        if (segments[position - 1].isAttribute) return unavailable()
        if (reference.lastSegmentContext == LocalReferenceContext.CALL_STATEMENT && position == segments.size) return unavailable()
        val steps = facts.resolutions[index]
        when (val first = unwrap(steps.first())) {
            LocalResolution.ProvenAbsent -> return NameBinding.NothingLocal(sqlColumnPossible = false)
            LocalResolution.SqlRelation -> return NameBinding.NothingLocal(sqlColumnPossible = true)
            else -> if (first is LocalResolution.Incomplete) return NameBinding.Unknown(reasons(first))
        }
        for (step in 0 until position) {
            when (val before = unwrap(steps.getOrNull(step) ?: return NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE)))) {
                is LocalResolution.Incomplete -> return NameBinding.Unknown(reasons(before))
                is LocalResolution.Bound -> if (model.declaration(before.declaration).kind !in QUALIFIERS) return unavailable()
                else -> return unavailable()
            }
        }
        return when (val own = unwrap(steps.getOrNull(position) ?: return NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE)))) {
            is LocalResolution.Incomplete -> NameBinding.Unknown(reasons(own))
            is LocalResolution.Field, is LocalResolution.CollectionMethod, is LocalResolution.Unsupported -> unavailable()
            else -> decided(own)
        }
    }

    /**
     * A qualified name (`fn.v`, `block.v`, `pkg.v`): the declaration its last segment binds, nothing local when its first
     * segment is not declared in the file, or Unknown (a record field or a member is not a declaration).
     */
    private fun qualified(name: AstNode): NameBinding {
        val index = qualifiedByStart[name.token.line to name.token.column] ?: return unavailable()
        val reference = facts.references[index]
        val written = name.children.count { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME } - 1
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return unavailable()
        val steps = facts.resolutions[index]
        val first = unwrap(steps.first())
        if (first == LocalResolution.ProvenAbsent) return NameBinding.NothingLocal(sqlColumnPossible = false)
        val last = unwrap(steps.last())
        return when {
            last is LocalResolution.Incomplete -> NameBinding.Unknown(reasons(last))
            steps.size != reference.segments.size + 1 -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
            last is LocalResolution.Bound -> NameBinding.Local(declaration(last.declaration))
            last == LocalResolution.SpecificationMember -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
            else -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
        }
    }

    private fun unwrap(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    /** A member of the specification in another file, the only reading besides a column. */
    private fun specificationMember(reference: LocalReference, outcome: LocalResolution.Incomplete, noColumn: Boolean): NameBinding? {
        if (outcome.provisional != LocalResolution.SpecificationMember || outcome.regionReasons.isNotEmpty() ||
            outcome.lookupReasons != setOf(LocalLookupGap.SQL_COLUMN_UNKNOWN)) return null
        val member = facts.sqlSpecificationMembers[reference.range.startLine to reference.range.startColumn] ?: return null
        val declaration = NameDeclaration(kind(member.kind), member.line, member.column, member.fileId)
        return if (noColumn) NameBinding.Local(declaration) else NameBinding.ColumnOrLocal(declaration)
    }

    /** Only a missing column scope keeps "nothing local" decided: the rest is Unknown. */
    private fun incomplete(outcome: LocalResolution.Incomplete, noColumn: Boolean): NameBinding {
        val provisional = unwrap(outcome.provisional)
        return if (outcome.regionReasons.isEmpty() && outcome.lookupReasons == setOf(LocalLookupGap.SQL_COLUMN_UNKNOWN) &&
            provisional == LocalResolution.ProvenAbsent
        ) {
            NameBinding.NothingLocal(sqlColumnPossible = !noColumn)
        } else {
            NameBinding.Unknown(reasons(outcome))
        }
    }

    private fun decided(outcome: LocalResolution): NameBinding = when (outcome) {
        LocalResolution.ProvenAbsent -> NameBinding.NothingLocal(sqlColumnPossible = false)
        is LocalResolution.Bound -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.OverloadSet -> NameBinding.Local(declaration(outcome.declarations.first()))
        is LocalResolution.LocalFunctionInSql -> NameBinding.Local(declaration(outcome.declarations.first()))
        // A declaration Oracle rejects here still takes the name.
        is LocalResolution.WrongKind -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.DeclaredLater -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.Duplicate -> NameBinding.Local(declaration(outcome.declarations.first()))
        is LocalResolution.OutOfScope -> NameBinding.Local(declaration(outcome.declaration))
        LocalResolution.SpecificationMember -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        else -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
    }

    private fun declaration(id: LocalDeclarationId): NameDeclaration {
        val declaration = model.declaration(id)
        return NameDeclaration(kind(declaration.kind), declaration.nameRange.startLine, declaration.nameRange.startColumn)
    }

    private fun kind(kind: LocalDeclarationKind) = when (kind) {
        LocalDeclarationKind.VARIABLE -> NameDeclarationKind.VARIABLE
        LocalDeclarationKind.PARAMETER, LocalDeclarationKind.CURSOR_PARAMETER -> NameDeclarationKind.PARAMETER
        LocalDeclarationKind.LOOP_INDEX -> NameDeclarationKind.LOOP_INDEX
        LocalDeclarationKind.FUNCTION -> NameDeclarationKind.FUNCTION
        LocalDeclarationKind.PROCEDURE -> NameDeclarationKind.PROCEDURE
        LocalDeclarationKind.CURSOR -> NameDeclarationKind.CURSOR
        LocalDeclarationKind.EXCEPTION -> NameDeclarationKind.EXCEPTION
        LocalDeclarationKind.SUBTYPE, LocalDeclarationKind.RECORD_TYPE, LocalDeclarationKind.COLLECTION_TYPE,
        LocalDeclarationKind.REF_CURSOR_TYPE, LocalDeclarationKind.OBJECT_TYPE -> NameDeclarationKind.TYPE
        LocalDeclarationKind.ATTRIBUTE -> NameDeclarationKind.ATTRIBUTE
        else -> NameDeclarationKind.OTHER
    }

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach {
            reasons += it.usageUncertainty()
        }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private fun unavailable() = NameBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)

    private companion object {
        /**
         * A plain name read in an expression, the name of a call in a PL/SQL expression (`f`, `f()`, `f(x)`), or the
         * cursor of `OPEN`, `FETCH` or `CLOSE`.
         */
        val CONTEXTS = EnumSet.of(
            LocalReferenceContext.VALUE, LocalReferenceContext.SQL_VALUE, LocalReferenceContext.INSERT_VALUE,
            LocalReferenceContext.CALL_EXPRESSION, LocalReferenceContext.CURSOR_OPERAND
        )

        /** The positions of a qualified name whose segments are answered: [CONTEXTS], and a call statement (`v.delete;`). */
        val CHAIN_CONTEXTS: EnumSet<LocalReferenceContext> = EnumSet.copyOf(CONTEXTS).apply { add(LocalReferenceContext.CALL_STATEMENT) }

        /** The declarations a segment can be reached through: a unit or a block, never a value. */
        val QUALIFIERS = EnumSet.of(
            LocalDeclarationKind.PACKAGE, LocalDeclarationKind.PACKAGE_BODY, LocalDeclarationKind.PROCEDURE, LocalDeclarationKind.FUNCTION,
            LocalDeclarationKind.LABEL, LocalDeclarationKind.TRIGGER, LocalDeclarationKind.TYPE_BODY
        )
    }
}
