/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api.checks

import com.sonar.sslr.api.AstNode
import com.sonar.sslr.api.AstNodeType
import com.sonar.sslr.api.Token
import com.sonar.sslr.api.Trivia
import org.sonar.plsqlopen.squid.PlSqlAstWalker
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext

open class PlSqlVisitor {

    lateinit var context: PlSqlVisitorContext
    private val astNodeTypesToVisit = mutableSetOf<AstNodeType>()

    fun subscribedKinds(): Set<AstNodeType> = astNodeTypesToVisit

    open fun startScan() {
        // default implementation does nothing
    }

    open fun init() {
        // default implementation does nothing
    }

    open fun visitFile(node: AstNode) {
        // default implementation does nothing
    }

    open fun leaveFile(node: AstNode) {
        // default implementation does nothing
    }

    open fun visitNode(node: AstNode) {
        // default implementation does nothing
    }

    open fun visitToken(token: Token) {
        // default implementation does nothing
    }

    open fun visitComment(trivia: Trivia, content: String) {
        // default implementation does nothing
    }

    open fun leaveNode(node: AstNode) {
        // default implementation does nothing
    }

    fun subscribeTo(vararg astNodeTypes: AstNodeType) {
        astNodeTypesToVisit.addAll(astNodeTypes)
    }

    fun scanFile(context: PlSqlVisitorContext) {
        val walker = PlSqlAstWalker(setOf(this))
        walker.walk(context)
    }

}
