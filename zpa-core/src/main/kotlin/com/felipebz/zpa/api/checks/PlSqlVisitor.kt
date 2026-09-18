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
package com.felipebz.zpa.api.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.rules.ZpaActiveRule
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.syntax.SyntaxView
import com.felipebz.zpa.api.syntax.SyntaxViewConsumer
import com.felipebz.zpa.api.syntax.SyntaxViewKind
import java.util.IdentityHashMap

@OptIn(ZpaExperimentalApi::class)
open class PlSqlVisitor {

    lateinit var context: PlSqlVisitorContext
    lateinit var activeRule: ZpaActiveRule
      internal set

    private val astNodeTypesToVisit = mutableSetOf<AstNodeType>()
    private val syntaxSubscriptions = IdentityHashMap<AstNodeType, IdentityHashMap<SyntaxViewKind<*>, SyntaxViewSubscription<*>>>()

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

    /** Subscribes to a typed view of the corresponding syntax construct. */
    @ZpaExperimentalApi
    fun <T : SyntaxView> subscribeTo(
        kind: SyntaxViewKind<T>,
        consumer: SyntaxViewConsumer<in T>
    ) {
        syntaxSubscriptions.getOrPut(kind.astNodeType()) { IdentityHashMap() }[kind] =
            SyntaxViewSubscription(kind, consumer)
    }

    internal fun syntaxViewSubscribedKinds(): Set<AstNodeType> = syntaxSubscriptions.keys

    internal fun resetSyntaxViewSubscriptions() {
        syntaxSubscriptions.clear()
    }

    internal fun dispatchSyntaxViews(node: AstNode) {
        syntaxSubscriptions[node.type]?.values?.forEach { it.dispatch(node) }
    }

    fun scanFile(context: PlSqlVisitorContext) {
        val walker = PlSqlAstWalker(setOf(this))
        walker.walk(context)
    }

}

@OptIn(ZpaExperimentalApi::class)
private class SyntaxViewSubscription<T : SyntaxView>(
    private val kind: SyntaxViewKind<T>,
    private val consumer: SyntaxViewConsumer<in T>
) {

    fun dispatch(node: AstNode) {
        consumer.accept(kind.createView(node))
    }
}
