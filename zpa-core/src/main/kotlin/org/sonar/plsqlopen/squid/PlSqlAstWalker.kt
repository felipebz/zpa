/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plsqlopen.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import org.sonar.plugins.plsqlopen.api.squid.PlSqlCommentAnalyzer
import java.util.*

class PlSqlAstWalker(private val checks: Collection<PlSqlVisitor>) {

    private val visitorsByNodeType = IdentityHashMap<AstNodeType, MutableList<PlSqlVisitor>>()
    private var lastVisitedToken: Token? = null

    fun walk(context: PlSqlVisitorContext) {
        for (check in checks) {
            check.context = context
            check.startScan()
            check.init()

            for (type in check.subscribedKinds()) {
                visitorsByNodeType.getOrPut(type) { mutableListOf() }.add(check)
            }
        }

        val tree = context.rootTree()
        if (tree != null) {
            try {
                for (check in checks) {
                    check.visitFile(tree)
                }
                visit(tree)
                for (check in checks) {
                    check.leaveFile(tree)
                }
            } catch (e: Exception) {
                val plsqlFile = context.plSqlFile()
                if (plsqlFile != null) {
                    throw AnalysisException("Error executing checks on file ${plsqlFile.fileName()}: ${e.message}", e)
                } else {
                    throw AnalysisException("Error executing checks: ${e.message}", e)
                }
            }
        }
    }

    private fun visit(ast: AstNode) {
        val nodeVisitors = getNodeVisitors(ast)
        visitNode(ast, nodeVisitors)
        visitToken(ast)
        visitChildren(ast)
        leaveNode(ast, nodeVisitors)
    }

    private fun leaveNode(ast: AstNode, nodeVisitors: List<PlSqlVisitor>) {
        for (i in nodeVisitors.indices.reversed()) {
            nodeVisitors[i].leaveNode(ast)
        }
    }

    private fun visitChildren(ast: AstNode) {
        for (child in ast.children) {
            visit(child)
        }
    }

    private fun visitToken(ast: AstNode) {
        if (ast.hasToken() && lastVisitedToken !== ast.token) {
            lastVisitedToken = ast.token
            for (astAndTokenVisitor in checks) {
                astAndTokenVisitor.visitToken(ast.token)

                for (trivia in ast.token.trivia) {
                    astAndTokenVisitor.visitComment(trivia,
                            PlSqlCommentAnalyzer.getContents(trivia.token.originalValue))
                }
            }
        }
    }

    private fun visitNode(ast: AstNode, nodeVisitors: List<PlSqlVisitor>) {
        for (nodeVisitor in nodeVisitors) {
            nodeVisitor.visitNode(ast)
        }
    }

    private fun getNodeVisitors(ast: AstNode) =
        visitorsByNodeType[ast.type] ?: emptyList()

}
