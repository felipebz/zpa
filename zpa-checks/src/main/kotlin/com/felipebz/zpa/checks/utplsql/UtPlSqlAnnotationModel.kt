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
package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.PlSqlCommentAnalyzer
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max

internal class UtPlSqlAnnotationGroup(
    val packageNode: AstNode,
    val declarationNode: AstNode?,
    annotations: List<UtPlSqlAnnotation>
) {
    val annotations: List<UtPlSqlAnnotation> = Collections.unmodifiableList(annotations.toList())
}

internal class UtPlSqlAnnotationModel internal constructor(
    groups: List<UtPlSqlAnnotationGroup>
) {
    val groups: List<UtPlSqlAnnotationGroup> = Collections.unmodifiableList(groups.toList())
}

internal object UtPlSqlAnnotationCollector {

    fun collect(root: AstNode): UtPlSqlAnnotationModel {
        val packageByToken = IdentityHashMap<Token, AstNode>()
        val declarationByToken = IdentityHashMap<Token, AstNode>()
        val packageStartByNode = IdentityHashMap<AstNode, Token>()
        indexNodes(root, null, null, packageByToken, declarationByToken, packageStartByNode)

        val groups = mutableListOf<UtPlSqlAnnotationGroup>()
        for (tokenIndex in root.tokens.indices) {
            val token = root.tokens[tokenIndex]
            val runs = annotationRuns(token, root.tokens.getOrNull(tokenIndex - 1))
            if (runs.isEmpty()) continue

            val packageNode = packageByToken[token] ?: continue
            if (packageStartByNode[packageNode] === token) continue
            val declaration = declarationByToken[token]
            runs.forEachIndexed { index, annotations ->
                val declarationNode = if (
                    declaration != null &&
                    index == runs.lastIndex &&
                    annotations.last().token.endLine + 1 == token.line
                ) declaration else null
                groups += UtPlSqlAnnotationGroup(packageNode, declarationNode, annotations)
            }
        }

        return UtPlSqlAnnotationModel(groups)
    }

    private fun indexNodes(
        node: AstNode,
        enclosingPackage: AstNode?,
        enclosingSubprogram: AstNode?,
        packageByToken: IdentityHashMap<Token, AstNode>,
        declarationByToken: IdentityHashMap<Token, AstNode>,
        packageStartByNode: IdentityHashMap<AstNode, Token>
    ) {
        val isPackage = node.type == PlSqlGrammar.CREATE_PACKAGE || node.type == PlSqlGrammar.CREATE_PACKAGE_BODY
        val packageNode = if (isPackage) {
            node
        } else {
            enclosingPackage
        }
        if (isPackage) {
            firstToken(node)?.let { packageStartByNode[node] = it }
        }
        val isSubprogram = node.type == PlSqlGrammar.PROCEDURE_DECLARATION || node.type == PlSqlGrammar.FUNCTION_DECLARATION
        if (isSubprogram && packageNode != null && enclosingSubprogram == null) {
            firstToken(node)?.let { declarationByToken[it] = node }
        }
        val subprogram = if (isSubprogram) node else enclosingSubprogram

        if (node.children.isEmpty()) {
            node.tokenOrNull?.let { token ->
                packageNode?.let { packageByToken[token] = it }
            }
        } else {
            node.children.forEach {
                indexNodes(it, packageNode, subprogram, packageByToken, declarationByToken, packageStartByNode)
            }
        }
    }

    private fun firstToken(node: AstNode): Token? {
        if (node.children.isEmpty()) return node.tokenOrNull
        return node.children.asSequence().mapNotNull(::firstToken).firstOrNull()
    }

    private fun annotationRuns(token: Token, previousToken: Token?): List<List<UtPlSqlAnnotation>> {
        val runs = mutableListOf<List<UtPlSqlAnnotation>>()
        var current = mutableListOf<UtPlSqlAnnotation>()
        var previousSourceLine = previousToken?.endLine ?: 0
        var previousAnnotationLine: Int? = null

        fun finishRun() {
            if (current.isNotEmpty()) {
                runs += current
                current = mutableListOf()
            }
        }

        for (trivia in token.trivia) {
            if (!trivia.isComment) {
                finishRun()
                previousSourceLine = max(previousSourceLine, trivia.tokens.maxOf { it.endLine })
                previousAnnotationLine = null
                continue
            }

            val commentToken = trivia.token
            val standaloneLine = commentToken.line > previousSourceLine
            if (!standaloneLine ||
                (previousAnnotationLine != null && commentToken.line > previousAnnotationLine + 1)
            ) {
                finishRun()
            }

            val annotation = if (standaloneLine) {
                UtPlSqlAnnotationParser.parse(
                    commentToken,
                    PlSqlCommentAnalyzer.getContents(commentToken.originalValue)
                )
            } else {
                null
            }
            if (annotation == null) {
                finishRun()
                previousAnnotationLine = null
            } else {
                current += annotation
                previousAnnotationLine = commentToken.endLine
            }
            previousSourceLine = max(previousSourceLine, commentToken.endLine)
        }
        finishRun()
        return runs
    }

}
