//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.SourceRange
import java.util.*

/**
 * What the plain names and call names written in expressions bind, and the segments of qualified names, from the
 * file's reference outcomes.
 */
internal class LocalNames(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val referencesByStart = referenceStarts(facts.references) { it.segments.isEmpty() && it.context in CONTEXTS }
    private val qualifiedByStart = referenceStarts(facts.references) { it.segments.isNotEmpty() && it.lastSegmentContext in CONTEXTS }

    /** The qualified names whose segments are answered one by one: those of [CONTEXTS], and method call statements. */
    private val chainsByStart = referenceStarts(facts.references) { it.segments.isNotEmpty() && it.lastSegmentContext in CHAIN_CONTEXTS }

    /** The segments after the first of those chains, by the start of their name: the reference ([segmentPosition] tells which segment). */
    private val segmentsByStart = segmentStarts(facts.references) { it.segments.isNotEmpty() && it.lastSegmentContext in CHAIN_CONTEXTS }

    /**
     * The names written with an argument list inside SQL (`v(1)`, `v(1).f`, `r.f(1)`, `o.m(x)`), by the start of their
     * first name: plain call names, and the first name of qualified call names.
     */
    private val sqlCallsByStart = referenceStarts(facts.references) { reference ->
        reference.context == LocalReferenceContext.SQL_CALL ||
            reference.context == LocalReferenceContext.QUALIFIER && reference.lastSegmentContext == LocalReferenceContext.SQL_CALL
    }

    fun binding(name: AstNode): NameBinding = when (name.type) {
        PlSqlGrammar.MEMBER_EXPRESSION -> qualified(name)
        PlSqlGrammar.VARIABLE_NAME -> {
            val line = name.token.line
            val column = name.token.column
            val index = referencesByStart[line, column]
            if (index >= 0) {
                first(index, facts.resolutions[index].last())
            } else {
                val chain = chainsByStart[line, column]
                val sqlCallIndex = if (chain >= 0) -1 else sqlCallsByStart[line, column]
                when {
                    chain >= 0 -> first(chain, facts.resolutions[chain].first())
                    sqlCallIndex >= 0 -> sqlCall(sqlCallIndex)
                    else -> unavailable()
                }
            }
        }
        PlSqlGrammar.IDENTIFIER_NAME -> {
            val index = segmentsByStart[name.token.line, name.token.column]
            if (index >= 0) segment(index, segmentPosition(facts.references[index], name.token.line, name.token.column)) else unavailable()
        }
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
            // A table, view or alias of the statement takes the first name of `x.f` when it has the column `f`.
            LocalResolution.SqlRelation -> facts.sqlRelationAlternatives[index]?.let { columnOr(first(index, it.first())) }
                ?: NameBinding.NothingLocal(sqlColumnPossible = true)
            else -> decided(decided, reference.range)
        }
    }

    /**
     * The first name of a name written with an argument list in SQL. A variable or parameter binds as in PL/SQL unless
     * a SQL built-in function has the name: the built-in is called whatever the regions declare. Neither a column nor
     * a relation of the statement takes `v(1)`; a relation takes the first name of `a.col.m()` (SqlRelation). A function,
     * a call of something no local declares, or any other declaration keeps no answer; a name the local side cannot
     * decide is Unknown.
     */
    private fun sqlCall(index: Int): NameBinding {
        val reference = facts.references[index]
        if (reference.segments.isEmpty() && SqlBuiltinFunctions.mayTake(reference.name)) return unavailable()
        return when (val outcome = unwrap(facts.resolutions[index].first())) {
            is LocalResolution.SqlBuiltinOrLocal, is LocalResolution.Bound -> value(outcome)?.let { NameBinding.Local(declaration(it)) } ?: unavailable()
            LocalResolution.SqlRelation -> NameBinding.NothingLocal(sqlColumnPossible = true)
            LocalResolution.SpecificationMember -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
            is LocalResolution.Incomplete -> {
                val provisional = unwrap(outcome.provisional)
                val mayBeValue = value(provisional) != null || provisional == LocalResolution.ProvenAbsent ||
                    provisional == LocalResolution.SpecificationMember
                if (mayBeValue) NameBinding.Unknown(reasons(outcome)) else unavailable()
            }
            else -> unavailable()
        }
    }

    /** The one variable, parameter or loop index [outcome] binds, or null. */
    private fun value(outcome: LocalResolution): LocalDeclarationId? {
        val declaration = when (outcome) {
            is LocalResolution.SqlBuiltinOrLocal -> outcome.declarations.singleOrNull()
            is LocalResolution.Bound -> outcome.declaration
            else -> null
        } ?: return null
        return declaration.takeIf { model.declaration(it).kind in VALUES }
    }

    /**
     * The segment at [position] of a qualified name: the declaration it binds when every segment before it names a unit
     * or a block (`pkg.v`, `pkg.g_rec.f`, `fn.p`). A segment after a value (a record field, a collection method, an
     * object member) or an attribute after `%` is not such a name, nor is the procedure a call statement names.
     */
    private fun segment(index: Int, position: Int, steps: List<LocalResolution> = facts.resolutions[index]): NameBinding {
        val reference = facts.references[index]
        val segments = reference.segments
        if (segments[position - 1].isAttribute) return unavailable()
        if (reference.lastSegmentContext == LocalReferenceContext.CALL_STATEMENT && position == segments.size) return unavailable()
        when (val first = unwrap(steps.first())) {
            LocalResolution.ProvenAbsent -> return NameBinding.NothingLocal(sqlColumnPossible = false)
            LocalResolution.SqlRelation -> return facts.sqlRelationAlternatives[index]?.let { columnOr(segment(index, position, it)) }
                ?: NameBinding.NothingLocal(sqlColumnPossible = true)
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
            else -> decided(own, segments[position - 1].range)
        }
    }

    /**
     * A qualified name (`fn.v`, `block.v`, `pkg.v`): the declaration its last segment binds, nothing local when its first
     * segment is not declared in the file, or Unknown (a record field or a member is not a declaration).
     */
    private fun qualified(name: AstNode): NameBinding {
        val index = qualifiedByStart[name.token.line, name.token.column]
        if (index < 0) return unavailable()
        val reference = facts.references[index]
        val written = name.children.count { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME } - 1
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return unavailable()
        val steps = facts.resolutions[index]
        if (unwrap(steps.first()) == LocalResolution.SqlRelation) {
            // A relation takes the first name; a declaration takes the whole name when the relation lacks the column.
            return facts.sqlRelationAlternatives[index]?.let { columnOr(qualifiedOutcome(reference, it)) } ?: NameBinding.NothingLocal(sqlColumnPossible = true)
        }
        return qualifiedOutcome(reference, steps)
    }

    private fun qualifiedOutcome(reference: LocalReference, steps: List<LocalResolution>): NameBinding {
        val first = unwrap(steps.first())
        if (first == LocalResolution.ProvenAbsent) return NameBinding.NothingLocal(sqlColumnPossible = false)
        val last = unwrap(steps.last())
        return when {
            last is LocalResolution.Incomplete -> NameBinding.Unknown(reasons(last))
            steps.size != reference.segments.size + 1 -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
            last is LocalResolution.Bound -> NameBinding.Local(declaration(last.declaration))
            last == LocalResolution.SpecificationMember -> decided(last, reference.segments.last().range)
            else -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
        }
    }

    /** The datatype names (`l_x t_rec;`, `pkg.t_rec`) by the start of their first name. */
    private val typesByStart = referenceStarts(facts.references) { it.lastSegmentContext == LocalReferenceContext.TYPE }

    /**
     * A datatype name, as written (a `VARIABLE_NAME`, or a `MEMBER_EXPRESSION` such as `pkg.t_rec`): the type declaration
     * of the file its whole name binds, nothing local when its first segment is not declared in the file, else Unknown.
     */
    fun typeBinding(name: AstNode): NameBinding {
        if (name.type != PlSqlGrammar.VARIABLE_NAME && name.type != PlSqlGrammar.MEMBER_EXPRESSION) return unavailable()
        val index = typesByStart[name.token.line, name.token.column]
        if (index < 0) return unavailable()
        val reference = facts.references[index]
        val written = if (name.type == PlSqlGrammar.MEMBER_EXPRESSION) {
            name.children.count { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME } - 1
        } else {
            0
        }
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return unavailable()
        val steps = facts.resolutions[index]
        if (unwrap(steps.first()) == LocalResolution.ProvenAbsent) return NameBinding.NothingLocal(sqlColumnPossible = false)
        val last = unwrap(steps.last())
        return when {
            last is LocalResolution.Incomplete -> NameBinding.Unknown(reasons(last))
            steps.size != reference.segments.size + 1 -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
            else -> decided(last, reference.segments.lastOrNull()?.range ?: reference.range)
        }
    }

    /** In SQL, after a relation of the statement: a declaration is only the reading when the relation lacks the column. */
    private fun columnOr(binding: NameBinding): NameBinding = when (binding) {
        is NameBinding.Local -> NameBinding.ColumnOrLocal(binding.declaration)
        is NameBinding.NothingLocal -> NameBinding.NothingLocal(sqlColumnPossible = true)
        else -> binding
    }

    private fun unwrap(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    /** A member of the specification in another file, the only reading besides a column. */
    private fun specificationMember(reference: LocalReference, outcome: LocalResolution.Incomplete, noColumn: Boolean): NameBinding? {
        if (outcome.provisional != LocalResolution.SpecificationMember || outcome.regionReasons.isNotEmpty() ||
            outcome.lookupReasons != setOf(LocalLookupGap.SQL_COLUMN_UNKNOWN)) return null
        val member = facts.specificationMembers[reference.range.startLine to reference.range.startColumn] ?: return null
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

    /** [outcome] of the name written at [range]: a specification member is the one the project names there, when one does. */
    private fun decided(outcome: LocalResolution, range: SourceRange): NameBinding = when (outcome) {
        LocalResolution.ProvenAbsent -> NameBinding.NothingLocal(sqlColumnPossible = false)
        is LocalResolution.Bound -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.OverloadSet -> NameBinding.Local(declaration(outcome.declarations.first()))
        is LocalResolution.LocalFunctionInSql -> NameBinding.Local(declaration(outcome.declarations.first()))
        // A declaration Oracle rejects here still takes the name.
        is LocalResolution.WrongKind -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.DeclaredLater -> NameBinding.Local(declaration(outcome.declaration))
        is LocalResolution.Duplicate -> NameBinding.Local(declaration(outcome.declarations.first()))
        is LocalResolution.OutOfScope -> NameBinding.Local(declaration(outcome.declaration))
        LocalResolution.SpecificationMember -> facts.specificationMembers[range.startLine to range.startColumn]
            ?.let { NameBinding.Local(NameDeclaration(kind(it.kind), it.line, it.column, it.fileId)) }
            ?: NameBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        else -> NameBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
    }

    fun declaration(id: LocalDeclarationId): NameDeclaration {
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

    companion object {
        /**
         * For each qualified name of SQL whose first segment a relation of the statement takes: its outcomes as a PL/SQL
         * name, by reference index. Oracle reads the column of that relation, and the PL/SQL name only when the relation
         * has no column of the next segment.
         */
        fun sqlRelationAlternatives(
            references: List<LocalReference>,
            resolutions: List<List<LocalResolution>>,
            outcomes: LocalReferenceOutcomes
        ): Map<Int, List<LocalResolution>> {
            val result = HashMap<Int, List<LocalResolution>>()
            for (index in references.indices) {
                if (resolutions[index].first() != LocalResolution.SqlRelation) continue
                val r = references[index]
                result[index] = outcomes.steps(LocalReference(r.name, r.range, r.context, r.region, r.segments, r.lastSegmentContext, r.inParameterDefault, r.handler, null, r.nameRange))
            }
            return result
        }

        /**
         * A plain name read in an expression, the name of a call in a PL/SQL expression (`f`, `f()`, `f(x)`), the
         * cursor of `OPEN`, `FETCH` or `CLOSE`, or the cursor (or value) a cursor `FOR` loop iterates over.
         */
        val CONTEXTS = EnumSet.of(
            LocalReferenceContext.VALUE, LocalReferenceContext.SQL_VALUE, LocalReferenceContext.INSERT_VALUE,
            LocalReferenceContext.CALL_EXPRESSION, LocalReferenceContext.CURSOR_OPERAND, LocalReferenceContext.FOR_CONTROL
        )

        /** The positions of a qualified name whose segments are answered: [CONTEXTS], and a call statement (`v.delete;`). */
        val CHAIN_CONTEXTS: EnumSet<LocalReferenceContext> = EnumSet.copyOf(CONTEXTS).apply { add(LocalReferenceContext.CALL_STATEMENT) }

        /** The declarations that hold a value an argument list can index or a member can follow. */
        private val VALUES = EnumSet.of(
            LocalDeclarationKind.VARIABLE, LocalDeclarationKind.PARAMETER, LocalDeclarationKind.CURSOR_PARAMETER, LocalDeclarationKind.LOOP_INDEX
        )

        /** The declarations a segment can be reached through: a unit or a block, never a value. */
        val QUALIFIERS = EnumSet.of(
            LocalDeclarationKind.PACKAGE, LocalDeclarationKind.PACKAGE_BODY, LocalDeclarationKind.PROCEDURE, LocalDeclarationKind.FUNCTION,
            LocalDeclarationKind.LABEL, LocalDeclarationKind.TRIGGER, LocalDeclarationKind.TYPE_BODY
        )
    }
}
