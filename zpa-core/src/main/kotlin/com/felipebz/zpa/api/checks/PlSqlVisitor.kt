//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.syntax.SyntaxView
import com.felipebz.zpa.api.syntax.SyntaxViewConsumer
import com.felipebz.zpa.api.syntax.SyntaxViewKind
import com.felipebz.zpa.rules.ZpaActiveRule
import com.felipebz.zpa.squid.PlSqlAstWalker
import java.util.*

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
