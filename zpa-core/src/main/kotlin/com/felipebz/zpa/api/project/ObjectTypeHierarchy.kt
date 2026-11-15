//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.project.IndexedDeclaration
import com.felipebz.zpa.project.StandaloneTypeDeclaration

/**
 * How two object types the project declares relate through their `UNDER` clauses. A value of an object type can be
 * passed to a formal of that type or of one of its supertypes, and to no other object type (PLS-00306); among
 * candidates that each take it, Oracle prefers the formal nearest to the value's own type.
 */
@OptIn(ZpaExperimentalApi::class)
internal class ObjectTypeHierarchy(private val snapshot: ProjectSnapshot) {
    /** A type followed by its supertypes, nearest first; [complete] when the walk reached a type that is under none. */
    private class Chain(val types: List<IndexedDeclaration>, val complete: Boolean)

    private val chains = HashMap<ProjectDeclarationId, Chain?>()

    /**
     * How many `UNDER` steps lead from the type [actual] up to [formal] (0 for the same type); null when [formal] is
     * not one of its types, or when either is not an object type of this snapshot.
     */
    fun distance(actual: ProjectDeclarationId, formal: ProjectDeclarationId): Int? {
        val chain = chain(actual) ?: return null
        val target = snapshot.occurrenceOf(formal) ?: return null
        return chain.types.indexOfFirst { it === target }.takeIf { it >= 0 }
    }

    /**
     * Whether a value of the type [actual] can be passed to a formal of the type [formal]: compatible when [formal] is
     * that type or one of its supertypes, incompatible when the whole supertype chain is known and does not reach it;
     * null when that cannot be told (a supertype the project does not hold once, a project with preparation failures).
     */
    fun outcome(actual: ProjectDeclarationId, formal: ProjectDeclarationId): Outcome? {
        if (distance(actual, formal) != null) return Outcome.COMPATIBLE
        val chain = chain(actual) ?: return null
        if (snapshot.occurrenceOf(formal)?.declaration !is StandaloneTypeDeclaration) return null
        return if (chain.complete && snapshot.failures.isEmpty()) Outcome.INCOMPATIBLE else null
    }

    private fun chain(id: ProjectDeclarationId): Chain? = chains.getOrPut(id) { walk(id) }

    private fun walk(id: ProjectDeclarationId): Chain? {
        val start = snapshot.occurrenceOf(id) ?: return null
        var type = start.declaration as? StandaloneTypeDeclaration ?: return null
        val types = arrayListOf(start)
        while (type.supertype != null) {
            val next = snapshot.projectIndex.supertypeOccurrences(type).singleOrNull() ?: return Chain(types, false)
            if (types.any { it === next }) return Chain(types, false)
            types += next
            type = next.declaration as StandaloneTypeDeclaration
        }
        return Chain(types, true)
    }
}
