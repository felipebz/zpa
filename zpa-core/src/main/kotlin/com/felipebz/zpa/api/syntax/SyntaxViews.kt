//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Public descriptors for the syntax views currently provided by ZPA.
 *
 * Use these descriptors with the typed [com.felipebz.zpa.api.checks.PlSqlVisitor.subscribeTo]
 * overload. Only selected parser constructs have views; the raw AST API
 * remains available for syntax that is not represented here.
 */
@ZpaExperimentalApi
public object SyntaxViews {

    /** A descriptor for SQL table references represented by the parser. */
    @JvmField
    @ZpaExperimentalApi
    public val TABLE_REFERENCE: SyntaxViewKind<TableReference> =
        SyntaxViewKind.create(DmlGrammar.TABLE_REFERENCE, ::AstTableReference)

    /**
     * A descriptor for generic METHOD_CALL syntax represented by the parser.
     * Specialized SQL-function grammar nodes are not included.
     */
    @JvmField
    @ZpaExperimentalApi
    public val METHOD_CALL: SyntaxViewKind<MethodCall> =
        SyntaxViewKind.create(PlSqlGrammar.METHOD_CALL, ::AstMethodCall)

    /**
     * A descriptor for local PL/SQL variable declarations represented by the
     * parser. EXCEPTION_DECLARATION is not included.
     */
    @JvmField
    @ZpaExperimentalApi
    public val VARIABLE_DECLARATION: SyntaxViewKind<VariableDeclaration> =
        SyntaxViewKind.create(PlSqlGrammar.VARIABLE_DECLARATION, ::AstVariableDeclaration)

    /** A descriptor for IF statements and their direct branches. */
    @JvmField
    @ZpaExperimentalApi
    public val IF_STATEMENT: SyntaxViewKind<IfStatement> =
        SyntaxViewKind.create(PlSqlGrammar.IF_STATEMENT, ::AstIfStatement)

    /**
     * A descriptor for PL/SQL SELECT statements and their top-level query
     * blocks. This is not a complete typed SQL AST.
     */
    @JvmField
    @ZpaExperimentalApi
    public val SELECT_STATEMENT: SyntaxViewKind<SelectStatement> =
        SyntaxViewKind.create(PlSqlGrammar.SELECT_STATEMENT, ::AstSelectStatement)
}

@OptIn(ZpaExperimentalApi::class)
private class AstTableReference(private val node: AstNode) : TableReference {

    private val components: List<String> by lazy {
        node.children
            .takeWhile { it.type !== PlSqlPunctuator.REMOTE }
            .filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
            .map { it.tokenOriginalValue }
    }

    private val linkComponents: List<String> by lazy {
        val remoteIndex = node.children.indexOfFirst { it.type === PlSqlPunctuator.REMOTE }
        if (remoteIndex < 0) {
            emptyList()
        } else {
            node.children.drop(remoteIndex + 1)
                .filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
                .map { it.tokenOriginalValue }
        }
    }

    override val astNode: AstNode
        get() = node

    override val schema: String?
        get() = components.dropLast(1).singleOrNull()

    override val name: String
        get() = components.lastOrNull().orEmpty()

    override val databaseLink: String?
        get() = linkComponents.takeIf { it.isNotEmpty() }?.joinToString(".")
}
