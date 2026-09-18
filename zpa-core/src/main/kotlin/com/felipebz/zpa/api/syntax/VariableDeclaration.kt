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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a [com.felipebz.zpa.api.PlSqlGrammar.VARIABLE_DECLARATION] node. */
@ZpaExperimentalApi
public interface VariableDeclaration : SyntaxView {

    /** The declaration name, preserving its source spelling. */
    public val name: String

    /** The identifier node for the declaration name. */
    public val nameAstNode: AstNode

    /** Whether the declaration has the direct CONSTANT modifier. */
    public val isConstant: Boolean

    /** The datatype node declared for this variable. */
    public val datatypeAstNode: AstNode

    /** The explicit nullability clause, if any. */
    public val nullability: VariableNullability

    /** The initializer syntax, or null when the declaration is not initialized. */
    public val initializerKind: VariableInitializerKind?

    /** The initializer expression node, or null when the declaration is not initialized. */
    public val initializerAstNode: AstNode?

}
