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
package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * A view of a generic [com.felipebz.zpa.api.PlSqlGrammar.METHOD_CALL] syntax node.
 *
 * This view exposes the syntactic call path only. It does not determine whether
 * qualifiers denote a schema, package, object, or another Oracle construct.
 * Specialized SQL-function grammar nodes are not represented by this view.
 */
@ZpaExperimentalApi
public interface MethodCall : SyntaxView {

    /** The target components before [name], preserving their original spelling. */
    public val qualifier: List<String>

    /** The final target component, preserving its original spelling. */
    public val name: String

    /** The database-link components after the target, or null when absent. */
    public val databaseLink: String?

    /** The argument lists represented by the call, in source order. */
    public val argumentLists: List<List<MethodCallArgument>>
}
