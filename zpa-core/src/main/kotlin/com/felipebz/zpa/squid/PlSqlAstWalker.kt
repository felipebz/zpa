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
package com.felipebz.zpa.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.squid.PlSqlCommentAnalyzer
import java.util.*

class PlSqlAstWalker(private val checks: Collection<PlSqlVisitor>) {

    private val visitorsByNodeType = IdentityHashMap<AstNodeType, MutableList<PlSqlVisitor>>()
    private val callbackVisitors = checks.mapNotNull { check ->
        val capabilities = callbackCapabilities.get(check.javaClass)
        if (capabilities.visitsToken || capabilities.visitsComment) {
            CallbackVisitor(check, capabilities.visitsToken, capabilities.visitsComment)
        } else {
            null
        }
    }
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
        val children = ast.children
        var index = 0
        while (index < children.size) {
            visit(children[index])
            index++
        }
    }

    private fun visitToken(ast: AstNode) {
        if (ast.hasToken() && lastVisitedToken !== ast.token) {
            lastVisitedToken = ast.token
            for (callbackVisitor in callbackVisitors) {
                if (callbackVisitor.visitsToken) {
                    callbackVisitor.visitor.visitToken(ast.token)
                }

                if (callbackVisitor.visitsComment) {
                    for (trivia in ast.token.trivia) {
                        callbackVisitor.visitor.visitComment(trivia,
                            PlSqlCommentAnalyzer.getContents(trivia.token.originalValue))
                    }
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

    private class CallbackVisitor(val visitor: PlSqlVisitor,
                                  val visitsToken: Boolean,
                                  val visitsComment: Boolean)

    private class CallbackCapabilities(val visitsToken: Boolean,
                                       val visitsComment: Boolean)

    private companion object {
        private val callbackCapabilities = object : ClassValue<CallbackCapabilities>() {
            override fun computeValue(type: Class<*>): CallbackCapabilities {
                val visitsToken = type.getMethod("visitToken", Token::class.java).declaringClass != PlSqlVisitor::class.java
                val visitsComment = type.getMethod("visitComment", Trivia::class.java, String::class.java).declaringClass != PlSqlVisitor::class.java
                return CallbackCapabilities(visitsToken, visitsComment)
            }
        }
    }

}
