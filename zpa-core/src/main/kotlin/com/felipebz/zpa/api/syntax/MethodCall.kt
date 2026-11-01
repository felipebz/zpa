//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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

    /**
     * The syntactic target components before [name], preserving their original
     * spelling. These are not classified as schema, package, or object names.
     */
    public val qualifier: List<String>

    /** The final syntactic target component, preserving its original spelling. */
    public val name: String

    /** The database-link suffix after the target, or null when absent. */
    public val databaseLink: String?

    /**
     * The argument lists represented by the call, in source order. Multiple
     * lists are kept separate when the parser represents them separately.
     */
    public val argumentLists: List<List<MethodCallArgument>>
}
