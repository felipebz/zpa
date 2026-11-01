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
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.ProjectReceiverMemberVisitor
import java.util.*

/**
 * Which field a segment written after a value names, from the resolutions zpa already holds: the local member steps
 * of a qualified name (a record type of this file), then the project member resolutions decorated on the tree (a
 * type of another file, a member after an element or a call result, a package variable of another file).
 */
internal class LocalFields(
    private val facts: LocalSemanticFacts,
    /** The prepared project's declarations, for the coverage of a type's specification; null when not prepared. */
    private val projectIndex: ProjectSymbolIndex?,
    /** The specification coverage of the file's package bodies; null when the project is not prepared. */
    private val coverage: LocalSpecificationCoverage? = null
) {
    private val model = facts.model

    /** The segments after the first of every qualified name, by the start of their name: the reference and the segment's position (1 for the second). */
    private val segmentsByStart: Map<Pair<Int, Int>, Pair<Int, Int>> by lazy(LazyThreadSafetyMode.NONE) { facts.references.indices
        .filter { facts.references[it].segments.isNotEmpty() && facts.references[it].context == LocalReferenceContext.QUALIFIER }
        .flatMap { index ->
            facts.references[index].segments.mapIndexed { position, segment -> (segment.range.startLine to segment.range.startColumn) to (index to position + 1) }
        }.toMap() }

    fun binding(node: AstNode): FieldBinding {
        val segment = if (node.type == PlSqlGrammar.IDENTIFIER_NAME && node.parentOrNull?.type == PlSqlGrammar.VARIABLE_NAME) node.parent else node
        if (segment.type != PlSqlGrammar.IDENTIFIER_NAME && segment.type != PlSqlGrammar.VARIABLE_NAME) return NOT_A_FIELD
        val located = segmentsByStart[segment.token.line to segment.token.column]
        val answer = answer(segment, located)
        if (answer !is FieldBinding.Field) return answer
        // A field is never more certain than the name its chain starts from.
        val start = located?.let { (index, position) -> facts.resolutions[index].take(position) } ?: startSteps(segment)
        return start?.let(::uncertainStart) ?: answer
    }

    private fun answer(segment: AstNode, located: Pair<Int, Int>?): FieldBinding {
        val local = located?.let { (index, position) -> local(index, position) }
        if (local != null && local !is Pending) return local.answer
        project(segment)?.let { return it }
        if (local is Pending) return local.answer
        return located?.let { (index, position) -> residual(index, position) } ?: afterReceiver(segment)
    }

    /** The steps of the name the expression around [segment] (an element, a call result) starts from, when one starts it. */
    private fun startSteps(segment: AstNode): List<LocalResolution>? {
        var outer = segment
        while (outer.parentOrNull?.type.let { it == PlSqlGrammar.MEMBER_EXPRESSION || it == PlSqlGrammar.METHOD_CALL || it == PlSqlGrammar.OBJECT_REFERENCE }) {
            outer = outer.parent
        }
        val first = outer.token
        return facts.steps(first.line, first.column)
    }

    /**
     * Unknown when the name the chain starts from (its first name, or the first after units and blocks) is undecided
     * because of text this file may hold (a `$IF`, a recovered region); a declaration outside the file does not count.
     */
    private fun uncertainStart(steps: List<LocalResolution>): FieldBinding? {
        for (step in steps) {
            when (val outcome = unwrap(step)) {
                is LocalResolution.Incomplete -> return if (outcome.regionReasons.isEmpty()) null else FieldBinding.Unknown(reasons(outcome))
                is LocalResolution.Bound -> if (model.declaration(outcome.declaration).kind !in LocalNames.QUALIFIERS) return null
                else -> return null
            }
        }
        return null
    }

    /** What the local member step says: [Decided] stands, [Pending] stands unless the project decides. */
    private sealed class LocalAnswer(val answer: FieldBinding)

    private class Decided(answer: FieldBinding) : LocalAnswer(answer)

    private class Pending(answer: FieldBinding) : LocalAnswer(answer)

    /** The local member step of the segment at [position]; null when it has none or does not type the value. */
    private fun local(index: Int, position: Int): LocalAnswer? {
        val reference = facts.references[index]
        if (reference.segments[position - 1].isAttribute) return Decided(NOT_A_FIELD)
        val steps = facts.resolutions[index]
        return when (val own = unwrap(steps.getOrNull(position) ?: return null)) {
            is LocalResolution.Field -> Decided(field(own))
            is LocalResolution.Bound -> Decided(attribute(own.declaration) ?: NOT_A_FIELD)
            is LocalResolution.Incomplete -> {
                val provisional = unwrap(own.provisional)
                when {
                    provisional is LocalResolution.Bound && attribute(provisional.declaration) == null -> Decided(NOT_A_FIELD)
                    // A record type of the package body that the specification, in another file, does not also declare.
                    provisional is LocalResolution.Field && own.regionReasons.isEmpty() &&
                        own.lookupReasons == setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE) &&
                        coverage?.notDeclaredBySpecification(provisional.recordType) == true -> Decided(field(provisional))
                    // Text this file may hold (a `$IF`, a recovered region): the project cannot see it either.
                    own.regionReasons.isNotEmpty() -> Decided(FieldBinding.Unknown(reasons(own)))
                    // A specification or supertype in another file: the project's member lists may decide.
                    else -> Pending(FieldBinding.Unknown(reasons(own)))
                }
            }
            is LocalResolution.CollectionMethod, is LocalResolution.WrongKind, LocalResolution.Undeclared -> Decided(NOT_A_FIELD)
            // The local resolver does not type the value: the project may.
            is LocalResolution.Unsupported -> null
            else -> Decided(NOT_A_FIELD)
        }
    }

    private fun field(step: LocalResolution.Field): FieldBinding {
        val field = (model.declaration(step.recordType).details as LocalRecordType).fields[step.ordinal]
        return FieldBinding.Field(FieldDeclaration(FieldDeclarationKind.RECORD_FIELD, step.ordinal, field.nameRange.startLine, field.nameRange.startColumn))
    }

    /** An attribute of an object type of this file, or null for another declaration. */
    private fun attribute(id: LocalDeclarationId): FieldBinding? {
        val declaration = model.declaration(id)
        if (declaration.kind != LocalDeclarationKind.ATTRIBUTE) return null
        val ordinal = model.region(declaration.region).declarations.filter { model.declaration(it).kind == LocalDeclarationKind.ATTRIBUTE }.indexOf(id)
        return FieldBinding.Field(FieldDeclaration(FieldDeclarationKind.ATTRIBUTE, ordinal, declaration.nameRange.startLine, declaration.nameRange.startColumn))
    }

    /** The project member resolution decorated for [segment], when there is one. */
    private fun project(segment: AstNode): FieldBinding? {
        val parent = segment.parentOrNull ?: return null
        if (segment.type == PlSqlGrammar.VARIABLE_NAME && ProjectReceiverMemberVisitor.isReceiverItem(segment)) {
            return (segment as SemanticAstNode).projectRecordMemberResolution?.let(::member)
        }
        if (parent.type != PlSqlGrammar.MEMBER_EXPRESSION) return null
        val names = parent.children.filter { it.type == PlSqlGrammar.VARIABLE_NAME || it.type == PlSqlGrammar.IDENTIFIER_NAME }
        val position = names.indexOfFirst { it === segment }
        val semantic = parent as SemanticAstNode
        // A member of an expression receiver: the path covers every name of the item, the first included.
        if (ProjectReceiverMemberVisitor.isReceiverItem(parent)) return semantic.projectRecordMemberPathResolution?.let { path(it, position) }
        if (position < 1) return null
        semantic.projectFieldMembers?.let { members ->
            if (position < members.firstMember) return null
            return members.member?.takeIf { position == members.firstMember }?.let(::member)
                ?: members.path?.let { path(it, position - members.firstMember) }
        }
        if (names.size == 2) semantic.projectRecordMemberResolution?.let { return member(it) }
        return semantic.projectRecordMemberPathResolution?.let { path(it, position - 1) }
    }

    private fun member(resolution: ProjectRecordMemberResolution): FieldBinding = when (resolution) {
        is ProjectRecordMemberResolution.Resolved -> projectField(resolution.declaration, resolution.field)
        is ProjectRecordMemberResolution.Method, is ProjectRecordMemberResolution.CollectionMethod -> NOT_A_FIELD
        else -> OUTSIDE_FILE
    }

    /** The answer for the member at [index] of [path] (0 for the first member it resolves). */
    private fun path(path: ProjectRecordMemberPathResolution, index: Int): FieldBinding {
        path.segments.getOrNull(index)?.let { return projectField(it.owner, it.field) }
        val stopped = path as? ProjectRecordMemberPathResolution.Stopped ?: return OUTSIDE_FILE
        if (index != stopped.nextMemberOrdinal) return OUTSIDE_FILE
        return when (stopped.reason) {
            ProjectRecordMemberPathResolution.StopReason.MEMBER_IS_METHOD,
            ProjectRecordMemberPathResolution.StopReason.MEMBER_IS_COLLECTION_METHOD -> NOT_A_FIELD
            else -> OUTSIDE_FILE
        }
    }

    private fun projectField(owner: ProjectTypeDeclaration?, field: ProjectRecordField): FieldBinding {
        uncertain(owner)?.let { return FieldBinding.Unknown(EnumSet.of(it)) }
        val range = field.sourceRange
        // Object attributes are extracted from standalone types, record fields from package types.
        val kind = if (owner is StandaloneTypeDeclaration) FieldDeclarationKind.ATTRIBUTE else FieldDeclarationKind.RECORD_FIELD
        val fileId = range.fileId.takeUnless { it == model.fileId }?.value
        return FieldBinding.Field(FieldDeclaration(kind, field.ordinal, range.startLine, range.startColumn, fileId))
    }

    /**
     * Why the fields the project lists for [owner] may not be those Oracle compiles: a package type written where its
     * specification has conditional-compilation text (the kept branch may not be compiled), an object type whose
     * attribute list was not fully read. Null when they are.
     */
    private fun uncertain(owner: ProjectTypeDeclaration?): UsageUncertainty? = when (owner) {
        is PackageTypeDeclaration -> {
            val index = projectIndex
            val specification = index?.occurrencesFor(owner.fileId)?.map { it.declaration }?.filterIsInstance<PackageDeclaration>()
                ?.firstOrNull { it.role == DeclarationRole.SPECIFICATION && it.name == owner.owner && contains(it.sourceRange, owner.sourceRange) }
            val names = specification?.memberNames
            when {
                names == null -> UsageUncertainty.INCOMPLETE_SOURCE
                names.isUnconditional(owner.sourceRange) -> null
                names.conditionalRange != null -> UsageUncertainty.CONDITIONAL_COMPILATION
                else -> UsageUncertainty.INCOMPLETE_SOURCE
            }
        }
        is StandaloneTypeDeclaration -> when {
            owner.conditional -> UsageUncertainty.CONDITIONAL_COMPILATION
            !owner.attributesComplete -> UsageUncertainty.INCOMPLETE_SOURCE
            else -> null
        }
        else -> UsageUncertainty.INCOMPLETE_SOURCE
    }

    private fun contains(outer: SourceRange, inner: SourceRange) =
        (outer.startLine < inner.startLine || outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn) &&
            (outer.endLine > inner.endLine || outer.endLine == inner.endLine && outer.endColumn >= inner.endColumn)

    /** Nothing decided the segment: whether it follows a value (undecided) or not (not a field). */
    private fun residual(index: Int, position: Int): FieldBinding {
        val steps = facts.resolutions[index]
        when (val first = unwrap(steps.first())) {
            // A relation of the statement: a column, or a relation qualified by its schema.
            LocalResolution.SqlRelation -> return NOT_A_FIELD
            // Nothing local: `other_pkg.v.f` may be a member of a package variable outside the project.
            LocalResolution.ProvenAbsent -> return if (position >= 2) OUTSIDE_FILE else NOT_A_FIELD
            is LocalResolution.Incomplete -> return FieldBinding.Unknown(reasons(first))
            else -> Unit
        }
        val before = steps.getOrNull(position - 1)?.let(::unwrap) ?: return UNSUPPORTED
        if (before is LocalResolution.Bound) {
            val declaration = model.declaration(before.declaration)
            if (declaration.kind !in VALUE_KINDS) return NOT_A_FIELD
            // The columns of a relation or cursor, not the fields of a record type.
            if ((declaration.typeRef as? AnchoredTypeRef)?.anchor == TypeAnchor.ROWTYPE) return NOT_A_FIELD
        }
        return UNSUPPORTED
    }

    /** A segment with no local reference: undecided when written after an expression receiver, else not a field. */
    private fun afterReceiver(segment: AstNode): FieldBinding {
        val item = if (segment.parent.type == PlSqlGrammar.MEMBER_EXPRESSION) segment.parent else segment
        return if (ProjectReceiverMemberVisitor.isReceiverItem(item) || ProjectReceiverMemberVisitor.isReceiverItem(segment)) UNSUPPORTED else NOT_A_FIELD
    }

    private fun unwrap(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach { reasons += it.usageUncertainty() }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private companion object {
        val NOT_A_FIELD = FieldBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)
        val OUTSIDE_FILE = FieldBinding.Unknown(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        val UNSUPPORTED = FieldBinding.Unknown(EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
        val VALUE_KINDS = EnumSet.of(
            LocalDeclarationKind.VARIABLE, LocalDeclarationKind.PARAMETER, LocalDeclarationKind.CURSOR_PARAMETER, LocalDeclarationKind.LOOP_INDEX
        )
    }
}
