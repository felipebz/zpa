//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.*
import java.util.*

/**
 * Which field a segment written after a value names, from the resolutions zpa already holds: the local member steps
 * of a qualified name (a record type of this file), then the member of the value the local type walk reaches before
 * the segment when the project declares that value's type (a type of another file, a member after an element or a call
 * result, a package variable of another file).
 */
internal class LocalFields(
    private val facts: LocalSemanticFacts,
    /** The prepared project's declarations, for the coverage of a type's specification; null when not prepared. */
    private val projectIndex: ProjectSymbolIndex?,
    /** The specification coverage of the file's package bodies; null when the project is not prepared. */
    private val coverage: LocalSpecificationCoverage? = null,
    /** The project's analysis context, for what `SELF.name` answers; null when the project is not prepared. */
    projectAnalysisContext: ProjectAnalysisContext? = null,
    /** The walk of local declared types, for a member after an element of a local collection. */
    private val typeWalk: LocalTypeWalk? = null,
    /** The values the walk reaches, for the value a segment is a member of. */
    private val categories: LocalValueCategories? = null
) {
    /** The prepared project, complete: what `SELF.name` reads; null when not prepared or a source failed. */
    private val completeIndex = (projectAnalysisContext?.state as? ProjectAnalysisContext.State.Prepared)?.result
        ?.takeIf { it.failures.isEmpty() }?.index
    private val memberResolver = projectAnalysisContext?.let(::ProjectRecordMemberResolver)
    private val model = facts.model

    /** The segments after the first of every qualified name, by the start of their name: the reference ([segmentPosition] tells which segment). */
    private val segmentsByStart: LocalPositionInts by lazy(LazyThreadSafetyMode.NONE) {
        segmentStarts(facts.references) { it.segments.isNotEmpty() && it.context == LocalReferenceContext.QUALIFIER }
    }

    fun binding(node: AstNode): FieldBinding {
        val segment = if (node.type == PlSqlGrammar.IDENTIFIER_NAME && node.parentOrNull?.type == PlSqlGrammar.VARIABLE_NAME) node.parent else node
        if (segment.type != PlSqlGrammar.IDENTIFIER_NAME && segment.type != PlSqlGrammar.VARIABLE_NAME) return NOT_A_FIELD
        // The reference whose segment this is and the segment's position (1 for the second name), or -1 and 0.
        val index = segmentsByStart[segment.token.line, segment.token.column]
        val position = if (index >= 0) segmentPosition(facts.references[index], segment.token.line, segment.token.column) else 0
        if (index < 0 && segment.type == PlSqlGrammar.VARIABLE_NAME) implicitSelf(segment)?.let { return it }
        val answer = answer(segment, index, position)
        if (answer !is FieldBinding.Field) return answer
        // A field is never more certain than the name its chain starts from.
        val start = if (index >= 0) facts.resolutions[index].take(position) else startSteps(segment)
        return start?.let(::uncertainStart) ?: answer
    }

    private fun answer(segment: AstNode, index: Int, position: Int): FieldBinding {
        val local = if (index >= 0) local(index, position) else null
        if (local != null && local !is Pending) return local.answer
        project(segment)?.let { return it }
        if (local is Pending) return local.answer
        return if (index >= 0) residual(index, position) else afterReceiver(segment)
    }

    /** The local record type [evidence] ends at, or null. */
    private fun recordOf(evidence: LocalTypeEvidence): LocalDeclarationId? = evidence.followed.lastOrNull()
        ?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.COMPOSITE && model.declaration(it).kind == LocalDeclarationKind.RECORD_TYPE }

    private fun fieldOrdinal(record: LocalDeclarationId, name: AstNode): Int? {
        val identifier = OracleIdentifier.fromToken(name.tokenOriginalValue, name.tokenValue)
        return (model.declaration(record).details as LocalRecordType).fields.indexOfFirst { it.name == identifier }.takeIf { it >= 0 }
    }

    /** The non-type references by the start of their first name: a plain name or the first name of a qualified one, alone at its start. */
    private val namesByStart: LocalPositionInts by lazy(LazyThreadSafetyMode.NONE) {
        val builder = LocalPositionInts.Builder()
        for ((index, reference) in facts.references.withIndex()) {
            if (reference.context !in NOT_VALUES) builder.put(reference.range.startLine, reference.range.startColumn, index)
        }
        builder.build(LocalPositionDuplicates.NONE)
    }

    /**
     * A name written alone, or as the first name of a chain, in a member method or a constructor of a type body: when
     * Oracle reads it as `SELF.name` (an attribute of the type, its own or a supertype's), it is answered as `SELF.name`
     * is, with the same declaration and no more certainty. Null when [segment] does not start such a reference, or the
     * reference is not in a type body: the caller answers as before. Anything else the name binds first (a parameter, a
     * local, a method) is not a field; when the file cannot decide, the answer is [FieldBinding.Unknown] unless the
     * project's lists of the type prove the name is no attribute.
     */
    private fun implicitSelf(segment: AstNode): FieldBinding? {
        if (segment.previousSiblingOrNull?.type == PlSqlPunctuator.DOT) return null
        val index = namesByStart[segment.token.line, segment.token.column]
        if (index < 0) return null
        val reference = facts.references[index]
        val chain = LocalResolver.chain(model, reference.region)
        val typeRegion = chain.firstOrNull { it.kind == LocalRegionKind.TYPE_BODY } ?: return null
        // No instance, no attribute: a static method, or code outside any method.
        val method = LocalTypeScope.method(model, chain)
        if (method == null || method.isStatic) return NOT_A_FIELD
        val first = facts.resolutions[index].first()
        val noColumn = reference.sql?.excludesColumn(reference.name) == true
        return when (val outcome = unwrap(first)) {
            // `items(1)`: an element of the attribute, which the local resolver does not type.
            is LocalResolution.Unsupported -> outcome.candidate?.takeIf { outcome.kind == LocalDeclarationKind.ATTRIBUTE }?.let(::attribute) ?: NOT_A_FIELD
            is LocalResolution.Bound -> attribute(outcome.declaration) ?: NOT_A_FIELD
            // Decided from the project's attribute and method lists of the type and its supertypes.
            LocalResolution.SpecificationMember -> selfMember(typeRegion, reference)
            // In SQL a column of a relation of the statement may take the name first.
            is LocalResolution.SqlColumnOrLocal -> {
                val field = attribute(outcome.declaration) ?: return NOT_A_FIELD
                if (noColumn) field else FieldBinding.Unknown(EnumSet.of(UsageUncertainty.SQL_NAME_MAY_BE_COLUMN))
            }
            is LocalResolution.Incomplete -> {
                val provisional = unwrap(outcome.provisional)
                val candidate = when {
                    provisional is LocalResolution.Bound -> attribute(provisional.declaration)
                    provisional is LocalResolution.SqlColumnOrLocal -> attribute(provisional.declaration)
                    // Nothing in the file takes the name first: the type's lists in the project, as for `SELF.name`.
                    provisional == LocalResolution.ProvenAbsent || provisional == LocalResolution.SpecificationMember ->
                        selfMember(typeRegion, reference)
                    else -> null
                }
                val gaps = if (noColumn) outcome.lookupReasons - LocalLookupGap.SQL_COLUMN_UNKNOWN else outcome.lookupReasons
                when {
                    candidate == null || candidate == NOT_A_FIELD -> NOT_A_FIELD
                    // Left only with the type's specification or a supertype in another file (over DUAL alone the
                    // column gap is closed): the project's answer stands.
                    outcome.regionReasons.isEmpty() && (provisional == LocalResolution.ProvenAbsent || provisional == LocalResolution.SpecificationMember) &&
                        TYPE_GAPS.containsAll(gaps) -> candidate
                    else -> FieldBinding.Unknown(reasons(outcome))
                }
            }
            else -> NOT_A_FIELD
        }
    }

    /**
     * What `SELF.name` answers from the project for the name [reference] starts with, in the type body [typeRegion]:
     * the attribute, not a field for a method or a name the type's complete lists do not have, and undecided when the
     * type is not in the project or a list may be incomplete (an `ALTER TYPE`, an unread list, a supertype not found).
     */
    private fun selfMember(typeRegion: LocalRegion, reference: LocalReference): FieldBinding {
        val typeName = LocalTypeScope.unitOf(model, typeRegion.id)?.unitName ?: return OUTSIDE_FILE
        val index = completeIndex ?: return OUTSIDE_FILE
        val members = memberResolver ?: return OUTSIDE_FILE
        // The body's own specification: the one of the same name in the same schema, whichever half writes the owner.
        val type = (index.counterparts(typeName, model.fileId).map { it.declaration }.filterIsInstance<StandaloneTypeDeclaration>()
            .singleOrNull())?.takeIf { it.shape == ProjectTypeShape.OBJECT } ?: return OUTSIDE_FILE
        return when (val resolution = members.resolve(type, reference.name)) {
            is ProjectRecordMemberResolution.NotFound -> NOT_A_FIELD
            else -> member(resolution)
        }
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

    private fun field(step: LocalResolution.Field): FieldBinding = FieldBinding.Field(fieldDeclaration(step.recordType, step.ordinal))

    private fun fieldDeclaration(recordType: LocalDeclarationId, ordinal: Int): FieldDeclaration {
        val field = (model.declaration(recordType).details as LocalRecordType).fields[ordinal]
        return FieldDeclaration(FieldDeclarationKind.RECORD_FIELD, ordinal, field.nameRange.startLine, field.nameRange.startColumn)
    }

    /** The record types of the file by the start of their name. */
    private val recordTypes: Map<Pair<Int, Int>, LocalDeclaration> by lazy(LazyThreadSafetyMode.NONE) {
        model.declarations.filter { it.details is LocalRecordType }.associateBy { it.nameRange.startLine to it.nameRange.startColumn }
    }

    /** The fields of the record type named at [typeName], as [binding] answers them; null when it names none. */
    fun recordFields(typeName: AstNode): List<DeclaredField>? = recordTypes[typeName.token.line to typeName.token.column]?.let { recordFields(it.id) }

    fun recordFields(type: LocalDeclarationId): List<DeclaredField>? {
        val record = model.declaration(type).details as? LocalRecordType ?: return null
        return record.fields.mapIndexed { ordinal, field -> DeclaredField(field.name.originalSpelling, fieldDeclaration(type, ordinal)) }
    }

    /** An attribute of an object type of this file, or null for another declaration. */
    private fun attribute(id: LocalDeclarationId): FieldBinding? {
        val declaration = model.declaration(id)
        if (declaration.kind != LocalDeclarationKind.ATTRIBUTE) return null
        val attributes = model.region(declaration.region).declarations.filter { model.declaration(it).kind == LocalDeclarationKind.ATTRIBUTE }
        // Hidden text in the attribute list may add or drop an attribute before this one, whatever it names: the
        // attribute's position is then undecided.
        val first = model.declaration(attributes.first()).range
        val last = model.declaration(attributes.last()).range
        val hidden = model.hiddenAt(SourceRange(first.fileId, first.startLine, first.startColumn, last.endLine, last.endColumn))
        if (hidden.isNotEmpty()) return FieldBinding.Unknown(hidden.mapTo(EnumSet.noneOf(UsageUncertainty::class.java)) { it.usageUncertainty() })
        val ordinal = attributes.indexOf(id)
        return FieldBinding.Field(FieldDeclaration(FieldDeclarationKind.ATTRIBUTE, ordinal, declaration.nameRange.startLine, declaration.nameRange.startColumn))
    }

    /**
     * The member [segment] names of the value the local type walk reaches before it. Of a record type of this file: its
     * field, wherever the walk reached the value (after an element or a call result, in SQL or not); a name the record
     * does not have is left to the caller. Of a local collection type: not a field. Of a type the project declares: a
     * field or attribute, not a field for a method or a collection method, and outside the file for a name the type
     * does not have once (or a type whose members are not known). Text this file may hold differently leaves the
     * value, and so the member, undecided. Null when the walk reaches no such value: an anchor, a value it does not
     * model.
     */
    private fun project(segment: AstNode): FieldBinding? {
        val walk = typeWalk ?: return null
        val (_, base) = categories?.memberBase(segment) ?: return null
        // The value is undecided by text this file may hold differently (a `$IF`, a substitution): so is its member.
        base.incomplete?.takeIf { base.terminal == LocalTypeEvidence.Terminal.UNKNOWN && it.regionReasons.isNotEmpty() }?.let { return FieldBinding.Unknown(reasons(it)) }
        // A value of a local collection type: its members are methods, or names it does not have (as the local steps say).
        val local = base.followed.lastOrNull()?.takeIf { base.terminal == LocalTypeEvidence.Terminal.COMPOSITE }?.let(model::declaration)
        if (local?.details is LocalCollectionType) return NOT_A_FIELD
        // A value of a record type of this file, wherever the walk reached it (an element, a call result, in SQL or not).
        recordOf(base)?.let { record -> return fieldOrdinal(record, segment)?.let { FieldBinding.Field(fieldDeclaration(record, it)) } }
        val name = LocalSyntax.identifier(segment.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: segment)
        walk.projectMember(base, name)?.let { return member(it) }
        // A value of a type the project names but does not declare (not found, a predefined type): no field of the file.
        val description = base.description ?: return null
        return OUTSIDE_FILE.takeIf { base.terminal == LocalTypeEvidence.Terminal.PROJECT && !description.anchor.isPresent }
    }

    private fun member(resolution: ProjectRecordMemberResolution): FieldBinding = when (resolution) {
        is ProjectRecordMemberResolution.Resolved -> projectField(resolution.declaration, resolution.field)
        is ProjectRecordMemberResolution.Method, is ProjectRecordMemberResolution.CollectionMethod -> NOT_A_FIELD
        else -> OUTSIDE_FILE
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
        return if (LocalSyntax.isReceiverItem(item) || LocalSyntax.isReceiverItem(segment)) UNSUPPORTED else NOT_A_FIELD
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
        /** Positions where a name is never a value of the instance: a datatype, an anchor, a call statement's procedure. */
        val NOT_VALUES = EnumSet.of(
            LocalReferenceContext.TYPE, LocalReferenceContext.ANCHOR_TYPE, LocalReferenceContext.ANCHOR_ROWTYPE, LocalReferenceContext.CALL_STATEMENT
        )
        val TYPE_GAPS: Set<LocalLookupGap> = EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE)
    }
}
