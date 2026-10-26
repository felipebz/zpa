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

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.semantics.HighlightingRange
import com.felipebz.zpa.project.*

/**
 * The highlighting pairs of a file from its local facts: each declaration's name and the names that refer to it.
 *
 * A reference is in a pair when its outcome binds a declaration (an erroneous use included), or when the outcome is
 * uncertain and has a provisional declaration. Each segment of a qualified name is its own reference; a member written
 * after a call (and the `*` of `t.*`) is never one. A type name never refers to a constructor. With [index], a name the
 * file proves absent refers to the schema object created earlier in the file when the project holds only that one.
 * A package body's name refers to its specification's name in the file. Nothing in another file is ever paired.
 *
 * The pairs respect a SonarQube symbol table: what had to go is counted in [Result.dropped].
 */
internal class LocalHighlighting(
    private val facts: LocalSemanticFacts,
    private val index: ProjectSymbolIndex?,
    /** The length of each line of the file, for range validation; null when the text is not known. */
    private val lineLengths: IntArray?
) {
    enum class Drop {
        INVALID_RANGE, OVERLAPS_DECLARATION, REPEATED_DECLARATION, SAME_START, MEMBER_POSITION, CONSTRUCTOR_FOR_TYPE
    }

    class Result(val pairs: List<HighlightingPair>, val dropped: Map<Drop, Pair<Int, String>>)

    private class Candidate(val range: SourceRange, val declaration: LocalRangeKey, val certain: Boolean)

    private val model = facts.model
    private val dropped = LinkedHashMap<Drop, Pair<Int, String>>()
    private val memberStarts = facts.members.mapTo(HashSet()) { it.range.startLine to it.range.startColumn }
    /** The file-level declarations (schema objects created by the file), by the range the project holds them at. */
    private val unitsByRange = model.root.declarations.map(model::declaration).associateBy { LocalRangeKey.of(it.range) }
    /** A package body linked to its specification in the file: its name is a reference to the specification's. */
    private val bodyToSpecification: Map<LocalDeclarationId, LocalDeclaration> = buildMap {
        val specifications = model.declarations.filter { it.kind == LocalDeclarationKind.PACKAGE }
            .associateBy { (it.details as? LocalUnit)?.region }
        for (body in model.declarations) {
            if (body.kind != LocalDeclarationKind.PACKAGE_BODY) continue
            val counterpart = (body.details as? LocalUnit)?.counterpart ?: continue
            specifications[counterpart]?.let { put(body.id, it) }
        }
    }

    private val declarations = LinkedHashMap<LocalRangeKey, SourceRange>()
    private val references = HashMap<Pair<Int, Int>, Candidate>()

    fun build(): Result {
        for (declaration in model.declarations) {
            if (declaration.id !in bodyToSpecification) addDeclaration(declaration.nameRange)
        }
        for ((body, specification) in bodyToSpecification) add(model.declaration(body).nameRange, specification to true)
        for ((index, reference) in facts.references.withIndex()) addReference(reference, facts.resolutions[index])
        val byDeclaration = references.values.groupBy { it.declaration }
        val pairs = declarations.map { (key, range) ->
            HighlightingPair(highlighting(range), byDeclaration[key].orEmpty().map { highlighting(it.range) }
                .sortedWith(compareBy({ it.startLine }, { it.startColumn })))
        }
        return Result(pairs, dropped)
    }

    private fun addDeclaration(range: SourceRange) {
        if (!valid(range)) drop(Drop.INVALID_RANGE, range)
        else if (declarations.putIfAbsent(LocalRangeKey.of(range), range) != null) drop(Drop.REPEATED_DECLARATION, range)
    }

    /** Each written segment of [reference] with its own outcome; an attribute or the `*` of `t.*` names nothing. */
    private fun addReference(reference: LocalReference, steps: List<LocalResolution>) {
        for ((step, outcome) in steps.withIndex()) {
            if (step == 0) {
                add(reference.nameRange, first(reference, outcome))
                continue
            }
            val segment = reference.segments.getOrNull(step - 1) ?: return
            if (segment.isAttribute || segment.name.originalSpelling == "*") continue
            add(segment.range, target(reference, outcome, step == reference.segments.size))
        }
    }

    private fun add(range: SourceRange?, target: Pair<LocalDeclaration, Boolean>?) {
        if (range == null || target == null) return
        val declaration = LocalRangeKey.of(target.first.nameRange)
        if (declaration !in declarations) return
        val start = range.startLine to range.startColumn
        when {
            start in memberStarts -> drop(Drop.MEMBER_POSITION, range)
            !valid(range) -> drop(Drop.INVALID_RANGE, range)
            overlaps(range, declaration) -> drop(Drop.OVERLAPS_DECLARATION, range)
            else -> keepOnePerStart(start, Candidate(range, declaration, target.second))
        }
    }

    /** SonarQube collapses references sharing a start: a certain one wins over a provisional one, else the first. */
    private fun keepOnePerStart(start: Pair<Int, Int>, candidate: Candidate) {
        val previous = references[start]
        if (previous == null || !previous.certain && candidate.certain) {
            if (previous != null) drop(Drop.SAME_START, previous.range)
            references[start] = candidate
        } else if (previous.declaration != candidate.declaration || LocalRangeKey.of(previous.range) != LocalRangeKey.of(candidate.range)) {
            drop(Drop.SAME_START, candidate.range)
        }
    }

    private fun first(reference: LocalReference, outcome: LocalResolution): Pair<LocalDeclaration, Boolean>? =
        target(reference, outcome, reference.segments.isEmpty())
            ?: if (LocalReferenceOutcomes.isAbsent(outcome)) schemaObject(reference)?.let { it to true } else null

    /** The declaration [outcome] names, and whether it is certain. */
    private fun target(reference: LocalReference, outcome: LocalResolution, whole: Boolean): Pair<LocalDeclaration, Boolean>? {
        val (id, certain) = declaration(reference, outcome, whole) ?: return null
        val declaration = model.declaration(id)
        if (isTypeName(reference) && (declaration.kind == LocalDeclarationKind.FUNCTION || declaration.kind == LocalDeclarationKind.PROCEDURE)) {
            drop(Drop.CONSTRUCTOR_FOR_TYPE, reference.nameRange ?: reference.range)
            return null
        }
        return (bodyToSpecification[id] ?: declaration) to certain
    }

    private fun declaration(reference: LocalReference, outcome: LocalResolution, whole: Boolean): Pair<LocalDeclarationId, Boolean>? =
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
            is LocalResolution.SpecificationCovered -> declaration(reference, outcome.outcome, whole)
            is LocalResolution.Incomplete -> declaration(reference, outcome.provisional, whole)?.let { it.first to false }
            is LocalResolution.Unsupported -> outcome.candidate?.let { it to false }
            else -> null
        }

    /** One overload, or the one the call resolution selects when the whole name is a call. */
    private fun overload(reference: LocalReference, declarations: List<LocalDeclarationId>, whole: Boolean): LocalDeclarationId? {
        declarations.singleOrNull()?.let { return it }
        if (!whole) return null
        val call = facts.localCall(reference.range.startLine, reference.range.startColumn) ?: return null
        return call.selected.takeIf { call.status == LocalCallResolution.Status.RESOLVED }
    }

    /** The schema unit of this file whose name is written before [reference], when the project holds only it under that name. */
    private fun schemaObject(reference: LocalReference): LocalDeclaration? {
        val index = index ?: return null
        val held = index.findOccurrences(QualifiedName(listOf(reference.name))).map { it.declaration }
            .filter { it.role != DeclarationRole.BODY }.distinct()
        val declaration = held.singleOrNull()?.takeIf { it.fileId == model.fileId } ?: return null
        val unit = unitsByRange[LocalRangeKey.of(declaration.sourceRange)] ?: return null
        // Created once its name is written: a reference inside the unit itself (recursion) counts.
        val name = unit.nameRange
        val earlier = name.startLine < reference.range.startLine ||
            name.startLine == reference.range.startLine && name.startColumn < reference.range.startColumn
        return unit.takeIf { earlier }
    }

    private fun valid(range: SourceRange): Boolean {
        if (range.startLine < 1 || range.endLine < range.startLine || range.startColumn < 0) return false
        if (range.endLine == range.startLine && range.endColumn <= range.startColumn) return false
        val lengths = lineLengths ?: return true
        return range.endLine <= lengths.size && range.startColumn <= lengths[range.startLine - 1] && range.endColumn <= lengths[range.endLine - 1]
    }

    private fun overlaps(range: SourceRange, declaration: LocalRangeKey): Boolean {
        fun before(line: Int, column: Int, otherLine: Int, otherColumn: Int) = line < otherLine || line == otherLine && column < otherColumn
        return before(range.startLine, range.startColumn, declaration.endLine, declaration.endColumn) &&
            before(declaration.startLine, declaration.startColumn, range.endLine, range.endColumn)
    }

    private fun drop(kind: Drop, range: SourceRange) {
        val previous = dropped[kind]
        dropped[kind] = ((previous?.first ?: 0) + 1) to (previous?.second ?: "${range.startLine}:${range.startColumn}")
    }

    private fun highlighting(range: SourceRange) = HighlightingRange(range.startLine, range.startColumn, range.endLine, range.endColumn)

    companion object {
        /** The project index a name proven absent may be found in, when [context] is prepared. */
        fun indexOf(context: ProjectAnalysisContext): ProjectSymbolIndex? =
            context.createSnapshot().takeIf { it.preparationState != ProjectPreparationState.NOT_PREPARED }?.projectIndex

        /** The file's pairs, as the scanner and the toolkit both build them. */
        fun pairs(facts: LocalSemanticFacts, index: ProjectSymbolIndex?, text: String?): List<HighlightingPair> =
            LocalHighlighting(facts, index, text?.let(::lineLengths)).build().pairs

        /** The lengths of the lines of [text], split as the lexer counts lines. */
        fun lineLengths(text: String): IntArray = text.split("\r\n", "\n", "\r").map { it.length }.toIntArray()
    }
}
