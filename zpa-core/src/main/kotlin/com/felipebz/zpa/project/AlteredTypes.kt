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
package com.felipebz.zpa.project

import java.util.*

/**
 * What `ALTER TYPE` statements of the project change, by type name as written: types whose attributes change, the
 * attribute names a `DROP ATTRIBUTE` removes, and types whose methods are added or dropped.
 */
internal class AlteredTypes(
    attributes: Set<QualifiedName> = emptySet(),
    droppedAttributes: Map<QualifiedName, Set<OracleIdentifier>> = emptyMap(),
    methods: Set<QualifiedName> = emptySet()
) {
    val attributes: Set<QualifiedName> = Collections.unmodifiableSet(HashSet(attributes))
    val droppedAttributes: Map<QualifiedName, Set<OracleIdentifier>> =
        Collections.unmodifiableMap(droppedAttributes.mapValues { Collections.unmodifiableSet(HashSet(it.value)) })
    val methods: Set<QualifiedName> = Collections.unmodifiableSet(HashSet(methods))

    fun isEmpty() = attributes.isEmpty() && methods.isEmpty()

    // Matched by the last name segment: the schema of an unqualified name is unknown.
    fun attributesOf(type: QualifiedName) = attributes.any { it.last == type.last }

    fun methodsOf(type: QualifiedName) = methods.any { it.last == type.last }

    fun dropped(type: QualifiedName, name: OracleIdentifier) =
        droppedAttributes.any { (altered, names) -> altered.last == type.last && name in names }

    override fun equals(other: Any?) = other is AlteredTypes &&
        attributes == other.attributes && droppedAttributes == other.droppedAttributes && methods == other.methods

    override fun hashCode() = Objects.hash(attributes, droppedAttributes, methods)

    companion object {
        val NONE = AlteredTypes()

        fun merge(all: Collection<AlteredTypes>): AlteredTypes {
            val dropped = HashMap<QualifiedName, MutableSet<OracleIdentifier>>()
            all.forEach { file -> file.droppedAttributes.forEach { (type, names) -> dropped.getOrPut(type) { HashSet() } += names } }
            return AlteredTypes(all.flatMapTo(HashSet()) { it.attributes }, dropped, all.flatMapTo(HashSet()) { it.methods })
        }
    }
}
