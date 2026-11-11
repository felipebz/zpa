//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.flr.impl.matcher.RuleDefinition
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.symbols.local.LocalFileAnalysis

/**
 * The FILE_INPUT root of a parsed file. It holds what the analysis of the file computes for the whole tree, so that
 * the other nodes pay nothing for it and the data lives exactly as long as the tree.
 */
internal class SemanticFileNode(type: AstNodeType, name: String, token: Token?) : SemanticAstNode(type, name, token) {

    /** The local analysis of the whole tree, left by the pre-pass; null when it failed or did not run. */
    var localAnalysis: LocalFileAnalysis? = null

    /** The project declaration of each immediate PACKAGE BODY subprogram that has one, by node. */
    private var subprogramDeclarations: HashMap<AstNode, PackageSubprogramDeclaration>? = null

    fun projectSubprogramDeclaration(node: AstNode): PackageSubprogramDeclaration? = subprogramDeclarations?.get(node)

    fun setProjectSubprogramDeclaration(node: AstNode, declaration: PackageSubprogramDeclaration) {
        val declarations = subprogramDeclarations ?: HashMap<AstNode, PackageSubprogramDeclaration>().also { subprogramDeclarations = it }
        declarations[node] = declaration
    }

    companion object {
        /** Builds a FILE_INPUT node as a [SemanticFileNode] and any other node as a [SemanticAstNode]. */
        fun create(type: AstNodeType, name: String, token: Token?): SemanticAstNode =
            if (type === PlSqlGrammar.FILE_INPUT || (type as? RuleDefinition)?.getRealAstNodeType() === PlSqlGrammar.FILE_INPUT) {
                SemanticFileNode(type, name, token)
            } else {
                SemanticAstNode(type, name, token)
            }

        /** The file node at the root of [node]'s tree; null for a tree whose root is not one. */
        fun of(node: AstNode): SemanticFileNode? {
            var root = node
            while (true) root = root.parentOrNull ?: break
            return root as? SemanticFileNode
        }
    }
}
