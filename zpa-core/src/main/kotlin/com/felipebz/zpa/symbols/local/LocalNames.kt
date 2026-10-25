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

/** What the plain names and call names written in expressions bind, from the file's reference outcomes. */
internal class LocalNames(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val referencesByStart = facts.references.indices
        .filter { facts.references[it].segments.isEmpty() && facts.references[it].context in CONTEXTS }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    fun binding(name: AstNode): NameBinding {
        if (name.type != PlSqlGrammar.VARIABLE_NAME) return unavailable()
        val index = referencesByStart[name.token.line to name.token.column] ?: return unavailable()
        val reference = facts.references[index]
        val outcome = facts.resolutions[index].last()
        // Over DUAL alone no column but DUMMY exists: the SQL reading is decided.
        val noColumn = reference.sql?.excludesColumn(reference.name) == true
        return when (val decided = unwrap(outcome)) {
            is LocalResolution.Incomplete -> specificationMember(reference, decided, noColumn) ?: incomplete(decided, noColumn)
            is LocalResolution.SqlColumnOrLocal ->
                if (noColumn) NameBinding.Local(declaration(decided.declaration)) else NameBinding.ColumnOrLocal(declaration(decided.declaration))
            else -> decided(decided)
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
            reasons += when (it) {
                LocalIncompleteness.CONDITIONAL_COMPILATION -> UsageUncertainty.CONDITIONAL_COMPILATION
                LocalIncompleteness.SUBSTITUTION -> UsageUncertainty.SUBSTITUTION
                LocalIncompleteness.PARSE_RECOVERY -> UsageUncertainty.PARSE_RECOVERY
                else -> UsageUncertainty.INCOMPLETE_SOURCE
            }
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
    }
}
