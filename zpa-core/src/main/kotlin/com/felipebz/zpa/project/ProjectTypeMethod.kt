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

enum class ProjectMethodKind {
    MEMBER,
    STATIC,
    CONSTRUCTOR,
    MAP,
    ORDER
}

/**
 * A method declared in an object type specification. [parameters] exclude an explicit `SELF` parameter, which is kept
 * apart in [selfParameter]; their ordinals count from 1 without it. [returnType] is null for a procedure and for a
 * constructor, which returns `SELF AS RESULT` ([returnsSelf]).
 */
class ProjectTypeMethod(
    val name: OracleIdentifier,
    val methodKind: ProjectMethodKind,
    val overriding: Boolean,
    val final: Boolean,
    val instantiable: Boolean,
    parameters: List<ProjectParameter>,
    val selfParameter: ProjectParameter?,
    val returnType: TypeRef?,
    val returnsSelf: Boolean,
    val sourceRange: SourceRange
) {
    val parameters: List<ProjectParameter> = immutableList(parameters)

    val isFunction: Boolean get() = returnType != null || returnsSelf

    init {
        require(!returnsSelf || (methodKind == ProjectMethodKind.CONSTRUCTOR && returnType == null))
    }

    /** Oracle overloads by parameter names and types; mode, defaults and the return type do not count. */
    fun overloadKey(): List<Pair<OracleIdentifier, String>> = parameters.map { it.name to it.typeRef.structuralKey() }

    override fun equals(other: Any?): Boolean = other is ProjectTypeMethod &&
        name == other.name && methodKind == other.methodKind && overriding == other.overriding && final == other.final &&
        instantiable == other.instantiable && parameters == other.parameters && selfParameter == other.selfParameter &&
        returnType == other.returnType && returnsSelf == other.returnsSelf && sourceRange == other.sourceRange

    override fun hashCode(): Int =
        listOf(name, methodKind, overriding, final, instantiable, parameters, selfParameter, returnType, returnsSelf, sourceRange).hashCode()

    override fun toString(): String =
        "ProjectTypeMethod($methodKind $name(${parameters.joinToString()})${returnType?.let { " RETURN $it" } ?: if (returnsSelf) " RETURN SELF AS RESULT" else ""})"
}
